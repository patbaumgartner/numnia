package ch.numnia.iam.service;

import ch.numnia.iam.domain.*;
import ch.numnia.iam.spi.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Handles child sign-in, sign-out, PIN management, and lockout release (UC-002).
 *
 * <p>Business rules enforced here:
 * <ul>
 *   <li>BR-001: children cannot use parent credentials (sign-in takes childId + PIN only).
 *   <li>BR-002: returned sessions have role {@code CHILD} only.
 *   <li>BR-003: PIN must be 4-6 digits, all numeric.
 *   <li>BR-004: after 5 consecutive failures the profile is locked; only the owning
 *       parent can release it.
 * </ul>
 *
 * <p>Security (NFR-SEC-003): PIN is stored as BCrypt hash only. Plain-text PIN
 * is discarded immediately after comparison.
 *
 * <p>Logging discipline (NFR-PRIV-001): no PII in log output. Parent referenced
 * by UUID, child by pseudonym or opaque UUID only.
 */
@Service
@Transactional
public class ChildSignInService {

    private static final Logger log = LoggerFactory.getLogger(ChildSignInService.class);

    /** Pattern for a valid PIN: 4-6 decimal digits, nothing else (BR-003). */
    private static final java.util.regex.Pattern PIN_PATTERN =
            java.util.regex.Pattern.compile("^\\d{4,6}$");

    private final ChildProfileRepository childProfiles;
    private final ParentAccountRepository parents;
    private final ChildSessionRepository sessions;
    private final AuditLogRepository auditLog;
    private final PasswordEncoder passwordEncoder;
    private final EmailGateway emailGateway;

    public ChildSignInService(ChildProfileRepository childProfiles,
                              ParentAccountRepository parents,
                              ChildSessionRepository sessions,
                              AuditLogRepository auditLog,
                              PasswordEncoder passwordEncoder,
                              EmailGateway emailGateway) {
        this.childProfiles = childProfiles;
        this.parents = parents;
        this.sessions = sessions;
        this.auditLog = auditLog;
        this.passwordEncoder = passwordEncoder;
        this.emailGateway = emailGateway;
    }

    /**
     * Sets (or replaces) the PIN for a child profile (UC-002 precondition).
     *
     * <p>Server-side validation (NFR-SEC-001):
     * <ul>
     *   <li>Parent must exist and own the child profile.
     *   <li>PIN must be 4-6 decimal digits (BR-003).
     *   <li>Child profile must be ACTIVE (double opt-in complete).
     * </ul>
     *
     * @param parentId  UUID of the parent requesting the PIN change
     * @param childId   UUID of the child profile
     * @param rawPin    Plain-text PIN — hashed immediately and not retained
     * @throws UnauthorizedParentException if the parent does not own the child profile
     * @throws ChildNotFoundException      if the child profile does not exist
     * @throws IllegalArgumentException    if the PIN does not meet format rules
     * @throws IllegalStateException       if the child profile is not ACTIVE
     */
    public void setPin(UUID parentId, UUID childId, String rawPin) {
        validatePinFormat(rawPin);

        ChildProfile profile = childProfiles.findById(childId)
                .orElseThrow(() -> new ChildNotFoundException("Child profile not found"));

        if (!profile.getParentId().equals(parentId)) {
            throw new UnauthorizedParentException(
                    "Parent is not the owner of this child profile");
        }
        if (profile.getStatus() != ChildStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Child profile must be ACTIVE before a PIN can be set");
        }

        String pinHash = passwordEncoder.encode(rawPin);
        profile.setPinHash(pinHash);
        childProfiles.save(profile);

        auditLog.save(new AuditLogEntry(
                AuditAction.CHILD_PIN_SET, parentId.toString(), profile.getPseudonym(), null));
        log.info("PIN set for child profile (parentRef={}, childRef={})",
                parentId, profile.getPseudonym());
    }

    /**
     * Signs a child in with the given PIN (UC-002 main flow steps 4-6).
     *
     * <p>Returns a new {@link ChildSession} with role {@code CHILD}. On wrong PIN,
     * increments the failed-attempt counter; after {@link ChildProfile#MAX_FAILED_ATTEMPTS}
     * consecutive failures the profile is locked (BR-004) and the parent is notified.
     *
     * <p>{@code noRollbackFor} for {@link InvalidPinException} and
     * {@link ProfileLockedException}: the failed-attempt counter and lock state
     * must be persisted even when a business-rule exception is thrown (the save
     * must NOT be rolled back, NFR-SEC-001).
     *
     * @param childId UUID of the child profile to sign in as
     * @param rawPin  Plain-text PIN — compared against BCrypt hash; not retained
     * @return a freshly created {@link ChildSession} with role {@code CHILD}
     * @throws ChildNotFoundException   if the child profile does not exist
     * @throws IllegalStateException    if no PIN has been set or the profile is not ACTIVE
     * @throws ProfileLockedException   if the profile is locked (BR-004)
     * @throws InvalidPinException      if the PIN is incorrect (increments counter)
     */
    @Transactional(noRollbackFor = {InvalidPinException.class, ProfileLockedException.class})
    public ChildSession signIn(UUID childId, String rawPin) {
        ChildProfile profile = childProfiles.findById(childId)
                .orElseThrow(() -> new ChildNotFoundException("Child profile not found"));

        if (profile.getStatus() != ChildStatus.ACTIVE) {
            throw new IllegalStateException("Child profile is not active");
        }
        if (!profile.hasPinSet()) {
            throw new IllegalStateException("No PIN has been set for this child profile");
        }

        // BR-004: reject immediately if already locked
        if (profile.isLocked()) {
            auditLog.save(new AuditLogEntry(
                    AuditAction.CHILD_SIGN_IN_FAILED,
                    profile.getParentId().toString(),
                    profile.getPseudonym(),
                    "PROFILE_LOCKED"));
            throw new ProfileLockedException(
                    "Child profile is locked — ask a parent to release it");
        }

        // Constant-time PIN comparison via BCrypt (NFR-SEC-003)
        if (!passwordEncoder.matches(rawPin, profile.getPinHash())) {
            boolean justLocked = profile.recordFailedSignIn();
            childProfiles.save(profile);

            auditLog.save(new AuditLogEntry(
                    AuditAction.CHILD_SIGN_IN_FAILED,
                    profile.getParentId().toString(),
                    profile.getPseudonym(),
                    "attempt=" + profile.getFailedSignInCount()));

            if (justLocked) {
                // BR-004: notify parent once per lock event
                auditLog.save(new AuditLogEntry(
                        AuditAction.CHILD_PROFILE_LOCKED,
                        profile.getParentId().toString(),
                        profile.getPseudonym(),
                        "TOO_MANY_FAILED_ATTEMPTS"));

                parents.findById(profile.getParentId()).ifPresent(parent ->
                        emailGateway.sendAccountLockedNotification(
                                parent.getEmail(),
                                profile.getPseudonym(),
                                profile.getId().toString()));

                log.info("Child profile locked after max failed attempts (childRef={})",
                        profile.getPseudonym());

                throw new ProfileLockedException(
                        "Child profile locked after too many failed attempts");
            }

            throw new InvalidPinException("Incorrect PIN — please try again");
        }

        // Successful sign-in
        profile.recordSuccessfulSignIn();
        childProfiles.save(profile);

        ChildSession session = new ChildSession(childId, profile.getParentId());
        sessions.save(session);

        auditLog.save(new AuditLogEntry(
                AuditAction.CHILD_SIGNED_IN,
                profile.getParentId().toString(),
                profile.getPseudonym(),
                null));
        log.info("Child signed in (childRef={})", profile.getPseudonym());

        return session;
    }

    /**
     * Signs the child out by revoking the active session (UC-002 main flow).
     *
     * @param sessionToken UUID token from the {@code X-Numnia-Session} header
     */
    public void signOut(UUID sessionToken) {
        sessions.findById(sessionToken).ifPresent(session -> {
            if (session.isValid()) {
                session.revoke();
                sessions.save(session);

                // Look up pseudonym for audit without logging PII
                childProfiles.findById(session.getChildId()).ifPresent(profile ->
                        auditLog.save(new AuditLogEntry(
                                AuditAction.CHILD_SIGNED_OUT,
                                session.getParentId().toString(),
                                profile.getPseudonym(),
                                null)));
                log.info("Child session revoked (parentRef={})", session.getParentId());
            }
        });
    }

    /**
     * Releases a lock on a child profile — only the owning parent may do this (UC-002).
     *
     * @param parentId UUID of the requesting parent
     * @param childId  UUID of the locked child profile
     * @throws ChildNotFoundException      if the child profile does not exist
     * @throws UnauthorizedParentException if the parent does not own the child profile
     * @throws IllegalStateException       if the profile is not locked
     */
    public void releaseLock(UUID parentId, UUID childId) {
        ChildProfile profile = childProfiles.findById(childId)
                .orElseThrow(() -> new ChildNotFoundException("Child profile not found"));

        if (!profile.getParentId().equals(parentId)) {
            throw new UnauthorizedParentException(
                    "Parent is not the owner of this child profile");
        }
        if (!profile.isLocked()) {
            throw new IllegalStateException("Child profile is not locked");
        }

        profile.releaseLock();
        childProfiles.save(profile);

        auditLog.save(new AuditLogEntry(
                AuditAction.CHILD_LOCK_RELEASED,
                parentId.toString(),
                profile.getPseudonym(),
                null));
        log.info("Child profile lock released (parentRef={}, childRef={})",
                parentId, profile.getPseudonym());
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private void validatePinFormat(String pin) {
        if (pin == null || !PIN_PATTERN.matcher(pin).matches()) {
            throw new IllegalArgumentException(
                    "PIN must be 4 to 6 digits (0-9) and contain no other characters");
        }
    }
}
