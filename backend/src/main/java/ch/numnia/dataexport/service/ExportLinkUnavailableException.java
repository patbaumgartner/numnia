package ch.numnia.dataexport.service;

/** Thrown when a download link no longer resolves to an available export file. */
public class ExportLinkUnavailableException extends RuntimeException {
    public ExportLinkUnavailableException(String message) {
        super(message);
    }
}
