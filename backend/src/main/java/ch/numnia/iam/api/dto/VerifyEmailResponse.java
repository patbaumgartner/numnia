package ch.numnia.iam.api.dto;

import java.util.UUID;

/**
 * Response body for {@code POST /api/parents/verify}.
 */
public record VerifyEmailResponse(UUID parentId, String status) {}
