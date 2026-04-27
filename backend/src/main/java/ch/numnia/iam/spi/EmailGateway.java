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
}
