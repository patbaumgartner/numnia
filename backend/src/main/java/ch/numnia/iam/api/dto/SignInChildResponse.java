package ch.numnia.iam.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response for a successful child sign-in (UC-002 main flow step 6).
 * Contains the session token to be sent as {@code X-Numnia-Session} in subsequent requests.
 */
public record SignInChildResponse(
        UUID sessionToken,
        UUID childId,
        String role,
        Instant expiresAt
) {}
