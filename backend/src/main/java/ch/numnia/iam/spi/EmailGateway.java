package ch.numnia.iam.spi;

/**
 * Port for outbound email notifications (UC-002, UC-001).
 *
 * <p>The production implementation uses a real SMTP relay (configured externally).
 * Tests use a no-op stub (see {@code IamConfig#emailGateway()}).
 *
 * <p>Privacy: implementations must not log email addresses or child real names
 * (NFR-PRIV-001). The {@code childPseudonym} and {@code childOpaqueRef} are
 * the only child identifiers permitted in notifications.
 */
public interface EmailGateway {

    /**
     * Sends an account-locked notification to the parent whose child profile
     * has been locked after too many failed sign-in attempts (UC-002 BR-004).
     *
     * @param parentEmail     Parent's email address — used as the send target only,
     *                        never logged
     * @param childPseudonym  Fantasy name (pseudonym) of the locked child profile
     * @param childOpaqueRef  Opaque child UUID string for identification — no real name
     */
    void sendAccountLockedNotification(String parentEmail,
                                       String childPseudonym,
                                       String childOpaqueRef);

    /**
     * Sends the deletion confirmation email containing the cool-off link
     * (UC-011 main flow step 3, BR-001). The email targets the parent's
     * verified address; implementations MUST never log the email address or
     * the confirmation token (NFR-PRIV-001).
     *
     * @param parentEmail        Parent's email address — send target, never logged
     * @param childPseudonym     Fantasy name (pseudonym) of the child to delete
     * @param confirmationToken  Opaque cool-off confirmation token — never logged
     */
    void sendDeletionConfirmationEmail(String parentEmail,
                                       String childPseudonym,
                                       String confirmationToken);

    /**
     * Sends the deletion record after a successful deletion (UC-011 BR-002:
     * date, subject, affected data categories). Implementations MUST never
     * log the email address (NFR-PRIV-001).
     *
     * @param parentEmail     Parent's email address — send target, never logged
     * @param childPseudonym  Fantasy name (pseudonym) of the deleted child
     * @param dataCategories  Set of data-category labels touched during deletion
     * @param completedAt     Timestamp at which deletion completed
     */
    void sendDeletionRecordEmail(String parentEmail,
                                 String childPseudonym,
                                 java.util.Set<String> dataCategories,
                                 java.time.Instant completedAt);
}
