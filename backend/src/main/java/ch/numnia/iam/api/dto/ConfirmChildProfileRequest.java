package ch.numnia.iam.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request body for {@code POST /api/parents/{parentId}/child-profiles/{childId}/confirm}.
 */
public record ConfirmChildProfileRequest(

        @NotNull(message = "Token is required")
        UUID token
) {}
