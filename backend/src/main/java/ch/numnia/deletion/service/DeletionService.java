package ch.numnia.deletion.service;

import ch.numnia.deletion.domain.DeletionRequest;
import ch.numnia.deletion.domain.DeletionStatus;
import ch.numnia.deletion.spi.ChildDataPurger;
import ch.numnia.deletion.spi.DeletionRequestRepository;
import ch.numnia.iam.domain.AuditAction;
import ch.numnia.iam.domain.AuditLogEntry;
import ch.numnia.iam.domain.ChildProfile;
import ch.numnia.iam.domain.ParentAccount;
import ch.numnia.iam.spi.AuditLogRepository;
import ch.numnia.iam.spi.ChildProfileRepository;
import ch.numnia.iam.spi.EmailGateway;
import ch.numnia.iam.spi.ParentAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * UC-011 — Parent self-service child-account deletion.
 *
 * <p>Server-side rules enforced:
 * <ul>
 *   <li>BR-001 deletion requires parent password, explicit "DELETE" word, and
 *       email cool-off confirmation within 24 hours.</li>
 *   <li>BR-002 deletion record (audit + email) carries date, subject and
 *       affected data categories.</li>
 *   <li>BR-003 statistical remainders must remain anonymised — purgers
 *       remove all child-scoped rows; aggregates do not carry the child id.</li>
 *   <li>BR-004 backups cleansed in the rotation window — modelled by the
 *       {@link AuditAction#DELETION_BACKUP_CLEANSED} entry that the backup
 *       sweeper writes (out of scope for this in-memory build).</li>
 *   <li>NFR-SEC-003 parent-ownership check on every operation.</li>
 *   <li>NFR-PRIV-001 only UUIDs / pseudonyms in audit + emails.</li>
 * </ul>
 */
@Service
public class DeletionService {

    private static final Logger log = LoggerFactory.getLogger(DeletionService.class);

    /** Cool-off window before the confirmation link expires (BR-001). */
    public static final Duration COOL_OFF_WINDOW = Duration.ofHours(24);

    /** Required confirmation word entered verbatim by the parent (BR-001). */
    public static final String CONFIRMATION_WORD = "DELETE";

    private final ChildProfileRepository childProfiles;
    private final ParentAccountRepository parents;
    private final DeletionRequestRepository deletionRequests;
    private final List<ChildDataPurger> purgers;
    private final AuditLogRepository auditLog;
    private final EmailGateway emailGateway;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public DeletionService(ChildProfileRepository childProfiles,
                           ParentAccountRepository parents,
                           DeletionRequestRepository deletionRequests,
                           List<ChildDataPurger> purgers,
                           AuditLogRepository auditLog,
                           EmailGateway emailGateway,
                           PasswordEncoder passwordEncoder) {
        this(childProfiles, parents, deletionRequests, purgers,
                auditLog, emailGateway, passwordEncoder, Clock.systemUTC());
    }

    public DeletionService(ChildProfileRepository childProfiles,
                           ParentAccountRepository parents,
                           DeletionRequestRepository deletionRequests,
                           List<ChildDataPurger> purgers,
                           AuditLogRepository auditLog,
                           EmailGateway emailGateway,
                           PasswordEncoder passwordEncoder,
                           Clock clock) {
        this.childProfiles = childProfiles;
        this.parents = parents;
        this.deletionRequests = deletionRequests;
        this.purgers = List.copyOf(purgers);
        this.auditLog = auditLog;
        this.emailGateway = emailGateway;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    // ── Public API ──────────────────────────────────────────────────────

    /**
     * Trigger deletion: validates ownership, password and confirmation word
     * (BR-001), persists a PENDING request with 24-hour expiry, sends the
     * confirmation email and audits {@code DELETION_REQUESTED}.
     */
    public DeletionRequest requestDeletion(UUID parentId,
                                           UUID childId,
                                           String rawPassword,
                                           String confirmationWord) {
        Objects.requireNonNull(parentId, "parentId");
        Objects.requireNonNull(childId, "childId");
        ChildProfile profile = requireOwnership(parentId, childId);
        ParentAccount parent = parents.findById(parentId)
                .orElseThrow(() -> new UnauthorizedDeletionAccessException(
                        "parent account not found"));

        if (!CONFIRMATION_WORD.equals(confirmationWord)) {
            throw new InvalidConfirmationWordException(
                    "confirmation word must be 'DELETE'");
        }
        if (rawPassword == null
                || !passwordEncoder.matches(rawPassword, parent.getHashedPassword())) {
            throw new InvalidPasswordException("invalid parent password");
        }

        Instant now = clock.instant();
        Instant expiresAt = now.plus(COOL_OFF_WINDOW);
        String token = randomToken();
        DeletionRequest request = new DeletionRequest(
                UUID.randomUUID(), parentId, childId, profile.getPseudonym(),
                token, now, expiresAt);
        deletionRequests.save(request);

        try {
            emailGateway.sendDeletionConfirmationEmail(
                    parent.getEmail(), profile.getPseudonym(), token);
        } catch (RuntimeException ex) {
            // Email failures must not leak PII into logs.
            log.error("Confirmation email delivery failed for childRef={}",
                    profile.getPseudonym(), ex);
            throw ex;
        }

        audit(AuditAction.DELETION_REQUESTED, parentId, profile,
                "requestId=" + request.id());
        return request;
    }

    /**
     * Confirm deletion: validates token + status + expiry, runs every
     * registered {@link ChildDataPurger}, deletes the {@link ChildProfile},
     * sends the deletion-record email and audits {@code DELETION_CONFIRMED}
     * with the comma-joined list of touched data categories (BR-002).
     */
    public DeletionRecord confirmDeletion(String token) {
        Objects.requireNonNull(token, "token");
        DeletionRequest request = deletionRequests.findByToken(token)
                .orElseThrow(() -> new DeletionLinkUnavailableException("unknown token"));
        if (request.status() != DeletionStatus.PENDING) {
            throw new DeletionLinkUnavailableException(
                    "deletion link is not pending (status=" + request.status() + ")");
        }
        Instant now = clock.instant();
        if (request.isExpiredAt(now)) {
            request.markDiscarded();
            deletionRequests.save(request);
            audit(AuditAction.DELETION_DISCARDED, request.parentId(),
                    request.childId(), request.childPseudonym(),
                    "requestId=" + request.id() + " reason=expired");
            throw new DeletionLinkUnavailableException("cool-off window has expired");
        }

        UUID childId = request.childId();
        Set<String> categoriesTouched = new TreeSet<>();
        for (ChildDataPurger purger : purgers) {
            Set<String> touched = purger.purge(childId);
            if (touched != null) {
                categoriesTouched.addAll(touched);
            }
        }

        ChildProfile profile = childProfiles.findById(childId).orElse(null);
        if (profile != null) {
            childProfiles.deleteById(childId);
            categoriesTouched.add("child-profile");
        }

        request.markCompleted(now);
        deletionRequests.save(request);

        ParentAccount parent = parents.findById(request.parentId()).orElse(null);
        Set<String> finalCategories = new LinkedHashSet<>(categoriesTouched);
        if (parent != null) {
            try {
                emailGateway.sendDeletionRecordEmail(
                        parent.getEmail(),
                        request.childPseudonym(),
                        finalCategories,
                        now);
            } catch (RuntimeException ex) {
                log.error("Deletion-record email delivery failed for childRef={}",
                        request.childPseudonym(), ex);
            }
        }

        audit(AuditAction.DELETION_CONFIRMED, request.parentId(),
                request.childId(), request.childPseudonym(),
                "requestId=" + request.id()
                        + " categories=" + String.join(",", finalCategories));
        return new DeletionRecord(request.id(), request.childPseudonym(), now, finalCategories);
    }

    /**
     * Sweep PENDING requests whose expiry has passed: mark each DISCARDED and
     * audit (UC-011 alt-flow 4a). Returns the number of requests transitioned.
     */
    public int expirePending() {
        Instant now = clock.instant();
        int discarded = 0;
        for (DeletionRequest request : deletionRequests.findAllByStatus(DeletionStatus.PENDING)) {
            if (request.isExpiredAt(now)) {
                request.markDiscarded();
                deletionRequests.save(request);
                audit(AuditAction.DELETION_DISCARDED, request.parentId(),
                        request.childId(), request.childPseudonym(),
                        "requestId=" + request.id() + " reason=expired");
                discarded++;
            }
        }
        return discarded;
    }

    // ── Internals ───────────────────────────────────────────────────────

    private ChildProfile requireOwnership(UUID parentId, UUID childId) {
        ChildProfile profile = childProfiles.findById(childId)
                .orElseThrow(() -> new UnauthorizedDeletionAccessException(
                        "child not found or not owned by parent"));
        if (!profile.getParentId().equals(parentId)) {
            throw new UnauthorizedDeletionAccessException(
                    "child not owned by requesting parent");
        }
        return profile;
    }

    private String randomToken() {
        byte[] buf = new byte[32];
        random.nextBytes(buf);
        StringBuilder sb = new StringBuilder(buf.length * 2);
        for (byte b : buf) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void audit(AuditAction action, UUID parentId, ChildProfile profile, String details) {
        String childRef = profile != null ? profile.getPseudonym() : null;
        auditLog.save(new AuditLogEntry(action, parentId.toString(), childRef, details));
    }

    private void audit(AuditAction action, UUID parentId, UUID childId,
                       String childPseudonym, String details) {
        // Privacy: audit by pseudonym only (NFR-PRIV-001).
        Objects.requireNonNull(childId, "childId");
        auditLog.save(new AuditLogEntry(action, parentId.toString(), childPseudonym, details));
    }

    /** Returned by {@link #confirmDeletion(String)} (BR-002). */
    public record DeletionRecord(UUID id,
                                 String childPseudonym,
                                 Instant completedAt,
                                 Set<String> dataCategories) {
    }
}
