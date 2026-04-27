package ch.numnia.dataexport.service;

/** Thrown when a parent attempts to export data of a child they do not own (NFR-SEC-003). */
public class UnauthorizedExportAccessException extends RuntimeException {
    public UnauthorizedExportAccessException(String message) {
        super(message);
    }
}
