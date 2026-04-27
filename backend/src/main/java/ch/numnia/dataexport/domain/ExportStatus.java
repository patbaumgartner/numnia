package ch.numnia.dataexport.domain;

/** Lifecycle status of a generated export file (UC-010 BR-002/BR-004). */
public enum ExportStatus {
    /** File is generated, signed URL active, deadline not yet reached. */
    AVAILABLE,
    /** File has been downloaded at least once. */
    DOWNLOADED,
    /** Deadline has passed without download; file is purged. */
    EXPIRED,
    /** Signed URL was invalidated (compromise / parent action). */
    INVALIDATED
}
