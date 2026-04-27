package ch.numnia.iam.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Single-use verification token for the double opt-in flow (BR-001).
 *
 * <p>The token value is the entity primary key (a random UUID). Tokens expire
 * after 24 hours ({@link #expiresAt}) and become unusable once consumed
 * ({@link #consumedAt} set). A null {@code consumedAt} means the token has
 * not been used yet.
 *
 * <p>Security note (NFR-SEC-002): tokens are single-use; the {@code consumedAt}
 * timestamp is set atomically when the token is verified.
 */
@Entity
@Table(name = "verification_tokens")
public class VerificationToken {

    /** The token value itself acts as the primary key (random UUID). */
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID parentId;

    /** Set for EMAIL_SECONDARY tokens; null for EMAIL_PRIMARY. */
    @Column
    private UUID childProfileId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenPurpose purpose;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column
    private Instant consumedAt;

    @Column(nullable = false)
    private Instant createdAt;

    protected VerificationToken() {
        // JPA
    }

    /**
     * Creates a new token expiring 24 hours from now.
     */
    public VerificationToken(UUID parentId, UUID childProfileId, TokenPurpose purpose) {
        this.id = UUID.randomUUID();
        this.parentId = parentId;
        this.childProfileId = childProfileId;
        this.purpose = purpose;
        this.createdAt = Instant.now();
        this.expiresAt = createdAt.plusSeconds(24 * 60 * 60);
    }

    /**
     * Creates a token with a custom expiry (for testing expired-token scenarios).
     */
    public VerificationToken(UUID parentId, UUID childProfileId, TokenPurpose purpose,
                             Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.parentId = parentId;
        this.childProfileId = childProfileId;
        this.purpose = purpose;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
    }

    public UUID getId() { return id; }
    public UUID getParentId() { return parentId; }
    public UUID getChildProfileId() { return childProfileId; }
    public TokenPurpose getPurpose() { return purpose; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getConsumedAt() { return consumedAt; }
    public Instant getCreatedAt() { return createdAt; }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public void consume() {
        this.consumedAt = Instant.now();
    }
}
