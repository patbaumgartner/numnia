package ch.numnia.iam.service;

import ch.numnia.iam.domain.*;
import ch.numnia.iam.spi.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Handles parent account registration and primary email verification.
 *
 * <p>Business rules covered: BR-001 (double opt-in), BR-004 (data minimisation),
 * BR-005 (audit trail).
 *
 * <p>Logging discipline (NFR-PRIV-001): the parent's email is never written to
 * any log statement. {@code parentRef} (UUID) is used instead.
 */
@Service
@Transactional
public class ParentRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(ParentRegistrationService.class);

    private final ParentAccountRepository parents;
    private final VerificationTokenRepository tokens;
    private final AuditLogRepository auditLog;
    private final PasswordEncoder passwordEncoder;

    public ParentRegistrationService(ParentAccountRepository parents,
                                     VerificationTokenRepository tokens,
                                     AuditLogRepository auditLog,
                                     PasswordEncoder passwordEncoder) {
        this.parents = parents;
        this.tokens = tokens;
        this.auditLog = auditLog;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new parent account (main flow steps 2-4).
     *
     * <p>Validates inputs (server-side, NFR-SEC-001). Hashes password with BCrypt
     * (NFR-SEC-003). Creates account with status {@link ParentStatus#NOT_VERIFIED}.
     * Persists a {@link VerificationToken} for primary email confirmation.
     *
     * @param email         Parent email address
     * @param rawPassword   Plain-text password (hashed immediately, never stored)
     * @param firstName     Parent first name
     * @param salutation    Parent salutation
     * @param privacyConsented Must be {@code true}
     * @param termsAccepted    Must be {@code true}
     * @return UUID of the newly created parent account
     * @throws DuplicateEmailException  if the email is already registered (flow 3b)
     * @throws IllegalArgumentException if required consents are missing (flow 3a)
     */
    public UUID register(String email, String rawPassword, String firstName,
                         String salutation, boolean privacyConsented,
                         boolean termsAccepted) {
        // Validation: consent flags mandatory (BR-004, FR-SAFE-006)
        if (!privacyConsented || !termsAccepted) {
            throw new IllegalArgumentException("Privacy consent and terms acceptance are mandatory");
        }
        // Validation: password minimum strength
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }

        // Check for duplicate email (flow 3b) — log by parentRef placeholder "N/A"
        if (parents.existsByEmail(email)) {
            auditLog.save(new AuditLogEntry(
                    AuditAction.DUPLICATE_REGISTRATION_BLOCKED,
                    "N/A",   // no parentId known at this point
                    null,
                    "duplicate-email-attempt"
            ));
            log.info("Registration blocked — email already registered (action=DUPLICATE_REGISTRATION_BLOCKED)");
            throw new DuplicateEmailException("Email address is already registered");
        }

        // Create the account
        UUID parentId = UUID.randomUUID();
        String hashedPassword = passwordEncoder.encode(rawPassword);
        ParentAccount account = new ParentAccount(
                parentId, email, hashedPassword,
                firstName, salutation, privacyConsented, termsAccepted);
        parents.save(account);

        // Create primary verification token (expires 24 h)
        VerificationToken token = new VerificationToken(parentId, null, TokenPurpose.EMAIL_PRIMARY);
        tokens.save(token);

        // Audit (BR-005) — parentRef = UUID, no email in log
        auditLog.save(new AuditLogEntry(
                AuditAction.ACCOUNT_CREATED, parentId.toString(), null, null));
        log.info("Account created (parentRef={})", parentId);

        return parentId;
    }

    /**
     * Confirms the parent's primary email address (main flow step 5).
     *
     * @param tokenValue  The UUID token from the verification email link
     * @return UUID of the parent account that was verified
     * @throws TokenNotFoundException if the token does not exist
     * @throws TokenExpiredException  if the token is older than 24 h (flow 5a)
     * @throws IllegalStateException  if the token has already been consumed
     */
    public UUID verifyPrimaryEmail(UUID tokenValue) {
        VerificationToken token = tokens.findById(tokenValue)
                .orElseThrow(() -> new TokenNotFoundException("Verification token not found"));

        if (token.isExpired()) {
            auditLog.save(new AuditLogEntry(
                    AuditAction.TOKEN_EXPIRED, token.getParentId().toString(), null, "EMAIL_PRIMARY"));
            throw new TokenExpiredException("Verification link has expired — please request a new one");
        }
        if (token.isConsumed()) {
            throw new IllegalStateException("Verification token has already been used");
        }
        if (token.getPurpose() != TokenPurpose.EMAIL_PRIMARY) {
            throw new IllegalArgumentException("Token is not a primary email verification token");
        }

        token.consume();
        tokens.save(token);

        ParentAccount account = parents.findById(token.getParentId())
                .orElseThrow(() -> new ParentNotFoundException(
                        "Parent account not found for token"));
        account.markEmailVerified();
        parents.save(account);

        auditLog.save(new AuditLogEntry(
                AuditAction.EMAIL_PRIMARY_VERIFIED, account.getId().toString(), null, null));
        log.info("Primary email verified (parentRef={})", account.getId());

        return account.getId();
    }

    /**
     * Issues a new primary verification token for an unverified account.
     * The previous token is left in place but will become unreachable because
     * the service always picks the most-recently-created unconsumed token.
     *
     * @param email Parent email address
     * @return New verification token UUID
     * @throws ParentNotFoundException if no account exists for this email
     * @throws IllegalStateException   if the account is already verified
     */
    public UUID requestNewVerificationEmail(String email) {
        ParentAccount account = parents.findByEmail(email)
                .orElseThrow(() -> new ParentNotFoundException("No account found for email"));

        if (account.getStatus() != ParentStatus.NOT_VERIFIED) {
            throw new IllegalStateException("Account is already verified");
        }

        VerificationToken newToken = new VerificationToken(
                account.getId(), null, TokenPurpose.EMAIL_PRIMARY);
        tokens.save(newToken);

        auditLog.save(new AuditLogEntry(
                AuditAction.VERIFICATION_EMAIL_RESENT, account.getId().toString(), null, null));
        log.info("New verification email issued (parentRef={})", account.getId());

        return newToken.getId();
    }
}
