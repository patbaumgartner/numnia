package ch.numnia.iam.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request body for child sign-in (UC-002 main flow step 4).
 * {@code POST /api/child-sessions}
 */
public record SignInChildRequest(
        @NotNull UUID childId,
        @NotBlank String pin
) {}
