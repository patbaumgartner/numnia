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
 */
@Entity
@Table(name = "child_profiles")
public class ChildProfile {

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
    }

    public UUID getId() { return id; }
    public String getPseudonym() { return pseudonym; }
    public int getYearOfBirth() { return yearOfBirth; }
    public String getAvatarBaseModel() { return avatarBaseModel; }
    public UUID getParentId() { return parentId; }
    public ChildStatus getStatus() { return status; }
    public boolean isMultiplayerEnabled() { return multiplayerEnabled; }
    public Instant getCreatedAt() { return createdAt; }

    public void activate() {
        this.status = ChildStatus.ACTIVE;
    }
}
