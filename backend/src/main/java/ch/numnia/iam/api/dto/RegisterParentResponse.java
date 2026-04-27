package ch.numnia.iam.api.dto;

import java.util.UUID;

/**
 * Response body for {@code POST /api/parents} — contains the new parent UUID.
 */
public record RegisterParentResponse(UUID parentId) {}
