package ch.numnia.iam.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Child profile — carries a pseudonym only, never a real name (BR-004, NFR-PRIV-001).
 *
 * <p>The pseudonym is a fantasy name chosen from the vetted catalog (BR-002).
 * Year of birth is restricted to the 7-12 age range (main flow step 7).
 * Avatar must come from the gender-neutral catalog (BR-003).
 *
 * <p>Multiplayer is persisted but disabled in Release 1 (main flow step 11).
 *
 * <p>UC-002 additions: PIN hash (BCrypt), failed sign-in counter, lockout timestamp.
 * Privacy: PIN is stored as BCrypt hash only — no plaintext ever persisted (NFR-SEC-003).
 */
@Entity
@Table(name = "child_profiles")
public class ChildProfile {

    /** Maximum consecutive failed sign-in attempts before lockout (BR-004). */
    public static final int MAX_FAILED_ATTEMPTS = 5;

    @Id
    private UUID id;

    /** Fantasy name from the vetted catalog — the pseudonym (BR-002, BR-004). */
    @Column(nullable = false)
    private String pseudonym;

    /** Year of birth — validated to produce an age in [7, 12]. */
    @Column(nullable = false)
    private int yearOfBirth;

    /** Gender-neutral avatar base model identifier (BR-003). */
    @Column(nullable = false)
    private String avatarBaseModel;

    /** Reference to the owning parent account (UUID only — no FK join loaded eagerly). */
    @Column(nullable = false)
    private UUID parentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChildStatus status;

    /**
     * Multiplayer / communication flag — persisted but always {@code false} in R1
     * (main flow step 11).
     */
    @Column(nullable = false)
    private boolean multiplayerEnabled;

    @Column(nullable = false)
    private Instant createdAt;

    // ── UC-002: PIN and lockout ──────────────────────────────────────────

    /**
     * BCrypt hash of the child's PIN. Null until the parent sets the PIN.
     * Plain-text PIN is NEVER stored (NFR-SEC-003, UC-002 BR-003).
     */
    @Column
    private String pinHash;

    /** Number of consecutive failed sign-in attempts (UC-002 BR-004). */
    @Column(nullable = false)
    private int failedSignInCount;

    /**
     * Timestamp at which the profile was locked due to too many failed attempts.
     * Null while the profile is not locked (UC-002 BR-004).
     */
    @Column
    private Instant lockedAt;

    /**
     * Human-readable reason for the lock — must not contain PII (NFR-PRIV-001).
     * Currently always {@code "TOO_MANY_FAILED_ATTEMPTS"}.
     */
    @Column
    private String lockedReason;

    protected ChildProfile() {
        // JPA
    }

    public ChildProfile(UUID id, String pseudonym, int yearOfBirth,
                        String avatarBaseModel, UUID parentId) {
        this.id = id;
        this.pseudonym = pseudonym;
        this.yearOfBirth = yearOfBirth;
        this.avatarBaseModel = avatarBaseModel;
        this.parentId = parentId;
        this.status = ChildStatus.PENDING_CONFIRM;
        this.multiplayerEnabled = false; // disabled in R1
        this.createdAt = Instant.now();
        this.failedSignInCount = 0;
    }

    public UUID getId() { return id; }
    public String getPseudonym() { return pseudonym; }
    public int getYearOfBirth() { return yearOfBirth; }
    public String getAvatarBaseModel() { return avatarBaseModel; }
    public UUID getParentId() { return parentId; }
    public ChildStatus getStatus() { return status; }
    public boolean isMultiplayerEnabled() { return multiplayerEnabled; }
    public Instant getCreatedAt() { return createdAt; }
    public String getPinHash() { return pinHash; }
    public int getFailedSignInCount() { return failedSignInCount; }
    public Instant getLockedAt() { return lockedAt; }
    public String getLockedReason() { return lockedReason; }

    /** Returns {@code true} if the profile is locked due to too many failed sign-in attempts. */
    public boolean isLocked() { return lockedAt != null; }

    /** Returns {@code true} if the parent has set a PIN for this profile. */
    public boolean hasPinSet() { return pinHash != null; }

    public void activate() {
        this.status = ChildStatus.ACTIVE;
    }

    /**
     * Stores the BCrypt hash of the PIN set by the parent (UC-002, NFR-SEC-003).
     * Resets the failed sign-in counter and lock state.
     *
     * @param pinHash BCrypt hash of the PIN — plain-text PIN must never reach this method
     */
    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
        this.failedSignInCount = 0;
        this.lockedAt = null;
        this.lockedReason = null;
    }

    /**
     * Records a successful sign-in — resets the failed attempt counter.
     */
    public void recordSuccessfulSignIn() {
        this.failedSignInCount = 0;
    }

    /**
     * Records a failed sign-in attempt. If the count reaches {@link #MAX_FAILED_ATTEMPTS},
     * the profile is locked (UC-002 BR-004).
     *
     * @return {@code true} if this failure triggered a lockout
     */
    public boolean recordFailedSignIn() {
        this.failedSignInCount++;
        if (this.failedSignInCount >= MAX_FAILED_ATTEMPTS && !isLocked()) {
            this.lockedAt = Instant.now();
            this.lockedReason = "TOO_MANY_FAILED_ATTEMPTS";
            return true;
        }
        return false;
    }

    /**
     * Releases the lockout — only callable by the owning parent (UC-002 main flow).
     * Resets the failed sign-in counter and clears the lock timestamp.
     */
    public void releaseLock() {
        this.lockedAt = null;
        this.lockedReason = null;
        this.failedSignInCount = 0;
    }
}

