package ch.numnia.iam.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Immutable audit log record for security-relevant events (BR-005, NFR-SEC-001).
 *
 * <p>Privacy rules: {@code parentRef} is the parent's internal UUID (never the email);
 * {@code childRef} is the child's pseudonym (never a real name). This ensures no PII
 * appears in the audit trail (NFR-PRIV-001).
 */
@Entity
@Table(name = "audit_log_entries")
public class AuditLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant timestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    /** Internal UUID of the parent — never the email address. */
    @Column(nullable = false)
    private String parentRef;

    /** Pseudonym of the affected child, or null if no child profile is involved. */
    @Column
    private String childRef;

    /** Optional supplementary detail — must not contain PII. */
    @Column
    private String details;

    protected AuditLogEntry() {
        // JPA
    }

    public AuditLogEntry(AuditAction action, String parentRef,
                         String childRef, String details) {
        this.timestamp = Instant.now();
        this.action = action;
        this.parentRef = parentRef;
        this.childRef = childRef;
        this.details = details;
    }

    public Long getId() { return id; }
    public Instant getTimestamp() { return timestamp; }
    public AuditAction getAction() { return action; }
    public String getParentRef() { return parentRef; }
    public String getChildRef() { return childRef; }
    public String getDetails() { return details; }
}
