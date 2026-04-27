package ch.numnia.iam.api.dto;

import java.util.UUID;

/**
 * Response body for {@code POST /api/parents/{parentId}/child-profiles}.
 */
public record CreateChildProfileResponse(UUID childProfileId, String pseudonym) {}
