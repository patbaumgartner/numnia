package ch.numnia.iam.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request body for {@code POST /api/parents/verify} — primary email confirmation.
 */
public record VerifyEmailRequest(

        @NotNull(message = "Token is required")
        UUID token
) {}
