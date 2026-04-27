package ch.numnia.iam.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Parent account aggregate root.
 *
 * <p>Privacy rules (NFR-PRIV-001, BR-004):
 * <ul>
 *   <li>Only data necessary for sign-in, consent and learning operations is stored.
 *   <li>The email address is stored for verification and sign-in only; it is never
 *       written to audit logs (logs reference {@code id} only).
 * </ul>
 *
 * <p>Passwords are stored exclusively as BCrypt hashes (NFR-SEC-001).
 */
@Entity
@Table(name = "parent_accounts")
public class ParentAccount {

    @Id
    private UUID id;

    /** Email address — stored for sign-in and verification; never logged. */
    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt-hashed password. Plain-text password is never persisted. */
    @Column(nullable = false)
    private String hashedPassword;

    @Column(nullable = false)
    private String firstName;

    /** Salutation — free-form, not used for identification. */
    @Column(nullable = false)
    private String salutation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParentStatus status;

    @Column(nullable = false)
    private boolean privacyConsented;

    @Column(nullable = false)
    private boolean termsAccepted;

    @Column(nullable = false)
    private Instant createdAt;

    protected ParentAccount() {
        // JPA
    }

    public ParentAccount(UUID id, String email, String hashedPassword,
                         String firstName, String salutation,
                         boolean privacyConsented, boolean termsAccepted) {
        this.id = id;
        this.email = email;
        this.hashedPassword = hashedPassword;
        this.firstName = firstName;
        this.salutation = salutation;
        this.status = ParentStatus.NOT_VERIFIED;
        this.privacyConsented = privacyConsented;
        this.termsAccepted = termsAccepted;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getHashedPassword() { return hashedPassword; }
    public String getFirstName() { return firstName; }
    public String getSalutation() { return salutation; }
    public ParentStatus getStatus() { return status; }
    public boolean isPrivacyConsented() { return privacyConsented; }
    public boolean isTermsAccepted() { return termsAccepted; }
    public Instant getCreatedAt() { return createdAt; }

    public void markEmailVerified() {
        this.status = ParentStatus.EMAIL_VERIFIED;
    }

    public void markFullyConsented() {
        this.status = ParentStatus.FULLY_CONSENTED;
    }
}
