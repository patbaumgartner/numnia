package ch.numnia.iam.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for setting or changing a child's PIN.
 * {@code POST /api/parents/{parentId}/child-profiles/{childId}/pin}
 */
public record SetChildPinRequest(
        @NotBlank String pin
) {}
