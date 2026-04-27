package ch.numnia.dataexport.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * A generated export artefact (UC-010).
 *
 * <p>Keys:
 * <ul>
 *   <li>{@code id} — internal identifier of the export request.</li>
 *   <li>{@code token} — opaque signed-URL token used by the download endpoint.
 *       The token is the only client-visible reference; {@code id} is internal.</li>
 *   <li>{@code expiresAt} — deadline after which the file is purged (BR-004).</li>
 *   <li>{@code downloadedAt} — set on first successful download (main flow 8).</li>
 *   <li>{@code status} — lifecycle marker.</li>
 * </ul>
 *
 * <p>Privacy: {@code childId} is the child's pseudonymous UUID; no PII is stored
 * on this entity (NFR-PRIV-001). The {@code content} byte array carries the
 * actual export payload (JSON UTF-8 bytes or a minimal PDF document).
 */
public final class ExportFile {

    private final UUID id;
    private final UUID parentId;
    private final UUID childId;
    private final ExportFormat format;
    private final String token;
    private final byte[] content;
    private final Instant createdAt;
    private final Instant expiresAt;
    private Instant downloadedAt;
    private ExportStatus status;

    public ExportFile(UUID id, UUID parentId, UUID childId, ExportFormat format,
                      String token, byte[] content,
                      Instant createdAt, Instant expiresAt) {
        if (id == null) throw new IllegalArgumentException("id must not be null");
        if (parentId == null) throw new IllegalArgumentException("parentId must not be null");
        if (childId == null) throw new IllegalArgumentException("childId must not be null");
        if (format == null) throw new IllegalArgumentException("format must not be null");
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token must not be blank");
        }
        if (content == null) throw new IllegalArgumentException("content must not be null");
        if (createdAt == null) throw new IllegalArgumentException("createdAt must not be null");
        if (expiresAt == null) throw new IllegalArgumentException("expiresAt must not be null");
        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("expiresAt must be after createdAt");
        }
        this.id = id;
        this.parentId = parentId;
        this.childId = childId;
        this.format = format;
        this.token = token;
        this.content = content.clone();
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.status = ExportStatus.AVAILABLE;
    }

    public UUID id() { return id; }
    public UUID parentId() { return parentId; }
    public UUID childId() { return childId; }
    public ExportFormat format() { return format; }
    public String token() { return token; }
    public byte[] content() { return content.clone(); }
    public int contentSize() { return content.length; }
    public Instant createdAt() { return createdAt; }
    public Instant expiresAt() { return expiresAt; }
    public Instant downloadedAt() { return downloadedAt; }
    public ExportStatus status() { return status; }

    public boolean isExpiredAt(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public void markDownloaded(Instant when) {
        if (this.downloadedAt == null) {
            this.downloadedAt = when;
        }
        this.status = ExportStatus.DOWNLOADED;
    }

    public void markExpired() {
        this.status = ExportStatus.EXPIRED;
    }

    public void invalidate() {
        this.status = ExportStatus.INVALIDATED;
    }

    /** Build the signed download URL for an external base URL. */
    public String signedUrl(String baseUrl) {
        return baseUrl + "/api/parents/me/exports/" + token;
    }
}
