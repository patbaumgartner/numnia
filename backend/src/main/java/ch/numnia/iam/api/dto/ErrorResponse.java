package ch.numnia.iam.api.dto;

/**
 * Generic error response returned by all exception handlers.
 */
public record ErrorResponse(String error, String message) {}
