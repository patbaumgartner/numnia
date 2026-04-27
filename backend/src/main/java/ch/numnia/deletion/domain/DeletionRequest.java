package ch.numnia.deletion.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks one parent-initiated child-account deletion across the cool-off
 * window (UC-011 BR-001).
 *
 * <p>Privacy: holds only UUIDs and the child's pseudonym (NFR-PRIV-001);
 * never email or real names. The opaque {@code token} is the cool-off
 * confirmation handle and must be ≥ 32 chars (BR-001).
 */
public final class DeletionRequest {

    private final UUID id;
    private final UUID parentId;
    private final UUID childId;
    private final String childPseudonym;
    private final String token;
    private final Instant createdAt;
    private Instant expiresAt;
    private Instant completedAt;
    private DeletionStatus status;

    public DeletionRequest(UUID id,
                           UUID parentId,
                           UUID childId,
                           String childPseudonym,
                           String token,
                           Instant createdAt,
                           Instant expiresAt) {
        if (id == null) throw new IllegalArgumentException("id must not be null");
        if (parentId == null) throw new IllegalArgumentException("parentId must not be null");
        if (childId == null) throw new IllegalArgumentException("childId must not be null");
        if (childPseudonym == null || childPseudonym.isBlank()) {
            throw new IllegalArgumentException("childPseudonym must not be blank");
        }
        if (token == null || token.length() < 32) {
            throw new IllegalArgumentException("token must be at least 32 chars (BR-001)");
        }
        if (createdAt == null) throw new IllegalArgumentException("createdAt must not be null");
        if (expiresAt == null) throw new IllegalArgumentException("expiresAt must not be null");
        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("expiresAt must be after createdAt");
        }
        this.id = id;
        this.parentId = parentId;
        this.childId = childId;
        this.childPseudonym = childPseudonym;
        this.token = token;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.status = DeletionStatus.PENDING;
    }

    public UUID id() { return id; }
    public UUID parentId() { return parentId; }
    public UUID childId() { return childId; }
    public String childPseudonym() { return childPseudonym; }
    public String token() { return token; }
    public Instant createdAt() { return createdAt; }
    public Instant expiresAt() { return expiresAt; }
    public Instant completedAt() { return completedAt; }
    public DeletionStatus status() { return status; }

    public boolean isExpiredAt(Instant now) {
        return !now.isBefore(expiresAt);
    }

    /** Test/operations hook to mark a request as expired before now (UC-011 alt-flow 4a). */
    public void overrideExpiresAt(Instant newExpiresAt) {
        if (newExpiresAt == null) throw new IllegalArgumentException("newExpiresAt must not be null");
        this.expiresAt = newExpiresAt;
    }

    public void markCompleted(Instant when) {
        this.status = DeletionStatus.COMPLETED;
        this.completedAt = when;
    }

    public void markDiscarded() {
        this.status = DeletionStatus.DISCARDED;
    }
}
