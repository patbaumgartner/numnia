package ch.numnia.iam.service;

import ch.numnia.iam.domain.*;
import ch.numnia.iam.spi.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Manages child profile creation and secondary email confirmation
 * (main flow steps 7-11, BR-002, BR-003).
 *
 * <p>Logging discipline (NFR-PRIV-001): child is logged by pseudonym only;
 * parent by UUID only — no real names or email addresses in log output.
 */
@Service
@Transactional
public class ChildProfileService {

    private static final Logger log = LoggerFactory.getLogger(ChildProfileService.class);

    /** Minimum and maximum ages in the target group (main flow step 7). */
    static final int MIN_AGE = 7;
    static final int MAX_AGE = 12;

    private final ParentAccountRepository parents;
    private final ChildProfileRepository childProfiles;
    private final VerificationTokenRepository tokens;
    private final AuditLogRepository auditLog;
    private final Set<String> fantasyNameCatalog;
    private final Set<String> avatarBaseModelCatalog;

    public ChildProfileService(ParentAccountRepository parents,
                               ChildProfileRepository childProfiles,
                               VerificationTokenRepository tokens,
                               AuditLogRepository auditLog,
                               Set<String> fantasyNameCatalog,
                               Set<String> avatarBaseModelCatalog) {
        this.parents = parents;
        this.childProfiles = childProfiles;
        this.tokens = tokens;
        this.auditLog = auditLog;
        this.fantasyNameCatalog = fantasyNameCatalog;
        this.avatarBaseModelCatalog = avatarBaseModelCatalog;
    }

    /**
     * Creates a child profile for a verified parent (main flow steps 7-9).
     *
     * <p>Validates (server-side, NFR-SEC-001):
     * <ul>
     *   <li>Parent account must exist and have status {@link ParentStatus#EMAIL_VERIFIED}
     *       or {@link ParentStatus#FULLY_CONSENTED}.
     *   <li>Fantasy name must be in the vetted catalog (BR-002).
     *   <li>Year of birth must produce an age in [7, 12] (main flow step 7, flow 7a).
     *   <li>Avatar must be in the gender-neutral catalog (BR-003).
     * </ul>
     *
     * @param parentId       Parent account UUID
     * @param pseudonym      Fantasy name from the vetted catalog
     * @param yearOfBirth    Year of birth (checked against 7-12 age range)
     * @param avatarBaseModel Avatar identifier from the gender-neutral catalog
     * @return UUID of the newly created child profile
     * @throws ParentNotFoundException      if the parent account does not exist
     * @throws IllegalStateException        if the parent email is not yet verified
     * @throws InvalidChildProfileException for any catalog or age rule violation
     */
    public UUID createChildProfile(UUID parentId, String pseudonym,
                                   int yearOfBirth, String avatarBaseModel) {
        // Validate parent
        ParentAccount parent = parents.findById(parentId)
                .orElseThrow(() -> new ParentNotFoundException("Parent not found"));

        if (parent.getStatus() == ParentStatus.NOT_VERIFIED) {
            throw new IllegalStateException("Parent email must be verified before creating a child profile");
        }

        // Validate fantasy name (BR-002)
        if (!fantasyNameCatalog.contains(pseudonym)) {
            throw new InvalidChildProfileException(
                    "Fantasy name is not in the vetted catalog (BR-002)");
        }

        // Validate age range (main flow step 7 / flow 7a)
        int currentYear = LocalDate.now().getYear();
        int age = currentYear - yearOfBirth;
        if (age < MIN_AGE || age > MAX_AGE) {
            throw new InvalidChildProfileException(
                    "Numnia is designed for ages 7-12 (age outside target group)");
        }

        // Validate avatar (BR-003)
        if (!avatarBaseModelCatalog.contains(avatarBaseModel)) {
            throw new InvalidChildProfileException(
                    "Avatar is not in the gender-neutral catalog (BR-003)");
        }

        // Create the child profile
        UUID childId = UUID.randomUUID();
        ChildProfile profile = new ChildProfile(
                childId, pseudonym, yearOfBirth, avatarBaseModel, parentId);
        childProfiles.save(profile);

        // Create secondary confirmation token
        VerificationToken secondaryToken = new VerificationToken(
                parentId, childId, TokenPurpose.EMAIL_SECONDARY);
        tokens.save(secondaryToken);

        // Audit (BR-005) — pseudonym as childRef, no real name
        auditLog.save(new AuditLogEntry(
                AuditAction.CHILD_PROFILE_CREATED, parentId.toString(), pseudonym, null));
        log.info("Child profile created (parentRef={}, childRef={})", parentId, pseudonym);

        return childId;
    }

    /**
     * Confirms the secondary consent for a child profile (main flow step 10-11).
     *
     * @param tokenValue UUID of the confirmation token from email #2
     * @throws TokenNotFoundException if the token does not exist
     * @throws TokenExpiredException  if the token has expired (flow 10a)
     * @throws IllegalStateException  if the token was already consumed
     */
    public void confirmChildProfile(UUID tokenValue) {
        VerificationToken token = tokens.findById(tokenValue)
                .orElseThrow(() -> new TokenNotFoundException("Confirmation token not found"));

        if (token.isExpired()) {
            auditLog.save(new AuditLogEntry(
                    AuditAction.TOKEN_EXPIRED, token.getParentId().toString(),
                    null, "EMAIL_SECONDARY"));
            throw new TokenExpiredException("Confirmation link has expired — please request a new one");
        }
        if (token.isConsumed()) {
            throw new IllegalStateException("Confirmation token has already been used");
        }
        if (token.getPurpose() != TokenPurpose.EMAIL_SECONDARY) {
            throw new IllegalArgumentException("Token is not a secondary confirmation token");
        }

        token.consume();
        tokens.save(token);

        // Activate child profile
        ChildProfile profile = childProfiles.findById(token.getChildProfileId())
                .orElseThrow(() -> new RuntimeException("Child profile not found for token"));
        profile.activate();
        childProfiles.save(profile);

        // Mark parent as fully consented
        ParentAccount parent = parents.findById(token.getParentId())
                .orElseThrow(() -> new ParentNotFoundException("Parent not found"));
        parent.markFullyConsented();
        parents.save(parent);

        auditLog.save(new AuditLogEntry(
                AuditAction.EMAIL_SECONDARY_CONFIRMED,
                parent.getId().toString(),
                profile.getPseudonym(),
                null));
        log.info("Secondary consent confirmed (parentRef={}, childRef={})",
                parent.getId(), profile.getPseudonym());
    }
}
