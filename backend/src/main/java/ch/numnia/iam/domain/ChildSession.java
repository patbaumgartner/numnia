package ch.numnia.iam.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Server-side child session (UC-002, BR-002).
 *
 * <p>The session token is the entity's UUID, sent to the client as the
 * {@code X-Numnia-Session} header. Sessions expire after 24 hours and can
 * be revoked (sign-out) before expiry.
 *
 * <p>Privacy: only {@code childId} (opaque UUID) and {@code parentId} (opaque UUID)
 * are stored — no real names, no email addresses (NFR-PRIV-001).
 */
@Entity
@Table(name = "child_sessions")
public class ChildSession {

    /** Default session lifetime — 24 hours. */
    private static final long SESSION_LIFETIME_SECONDS = 24L * 60 * 60;

    /**
     * Session token — the opaque value the client sends in
     * {@code X-Numnia-Session}. Acts as the primary key.
     */
    @Id
    private UUID id;

    /** Opaque child profile UUID (UC-002 BR-002, NFR-PRIV-001). */
    @Column(nullable = false)
    private UUID childId;

    /** Owning parent UUID — used for cross-area authz checks. */
    @Column(nullable = false)
    private UUID parentId;

    /**
     * Role of this session. Always {@code "CHILD"} for profiles signed in
     * via the child sign-in flow (UC-002 BR-002, BR-001).
     */
    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    /**
     * Set when the child explicitly signs out (UC-002 main flow).
     * Null while the session is active.
     */
    @Column
    private Instant revokedAt;

    protected ChildSession() {
        // JPA
    }

    /**
     * Creates a new CHILD-role session with a 24-hour lifetime.
     *
     * @param childId  Opaque child profile UUID
     * @param parentId Owning parent UUID
     */
    public ChildSession(UUID childId, UUID parentId) {
        this.id = UUID.randomUUID();
        this.childId = childId;
        this.parentId = parentId;
        this.role = "CHILD";
        this.createdAt = Instant.now();
        this.expiresAt = createdAt.plusSeconds(SESSION_LIFETIME_SECONDS);
    }

    public UUID getId() { return id; }
    public UUID getChildId() { return childId; }
    public UUID getParentId() { return parentId; }
    public String getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }

    /** Returns {@code true} if the session is still valid (not expired and not revoked). */
    public boolean isValid() {
        return revokedAt == null && Instant.now().isBefore(expiresAt);
    }

    /** Invalidates the session (sign-out). */
    public void revoke() {
        this.revokedAt = Instant.now();
    }
}
