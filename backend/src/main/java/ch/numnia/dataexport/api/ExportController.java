package ch.numnia.dataexport.api;

import ch.numnia.dataexport.domain.ExportFile;
import ch.numnia.dataexport.domain.ExportFormat;
import ch.numnia.dataexport.service.ExportLinkUnavailableException;
import ch.numnia.dataexport.service.ExportService;
import ch.numnia.dataexport.service.UnauthorizedExportAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * UC-010 — Parent self-service data export endpoint.
 *
 * <p>Auth: parent identification still travels via the {@code X-Parent-Id}
 * placeholder header until UC-009/UC-001 wires Spring Security on the
 * parent area. Server-side ownership enforcement is performed in the
 * service (NFR-SEC-003).
 */
@RestController
@RequestMapping("/api/parents/me")
public class ExportController {

    private final ExportService service;

    public ExportController(ExportService service) {
        this.service = service;
    }

    /** Trigger an export. Returns metadata + signed download URL token. */
    @PostMapping("/children/{childId}/exports")
    public ResponseEntity<List<ExportSummary>> trigger(
            @RequestHeader("X-Parent-Id") UUID parentId,
            @PathVariable UUID childId,
            @RequestBody TriggerRequest body) {
        ExportFormat format = body != null && body.format != null ? body.format : ExportFormat.JSON;
        List<ExportFile> generated = service.requestExport(parentId, childId, format);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(generated.stream().map(ExportSummary::from).toList());
    }

    /** Download a previously triggered export by its signed-URL token. */
    @GetMapping("/exports/{token}")
    public ResponseEntity<byte[]> download(
            @RequestHeader("X-Parent-Id") UUID parentId,
            @PathVariable String token) {
        ExportFile file = service.download(token);
        if (!file.parentId().equals(parentId)) {
            throw new UnauthorizedExportAccessException(
                    "export does not belong to requesting parent");
        }
        MediaType mediaType = file.format() == ExportFormat.PDF
                ? MediaType.APPLICATION_PDF
                : MediaType.APPLICATION_JSON;
        String filename = "numnia-export-" + file.id() + extensionFor(file.format());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(file.content());
    }

    @ExceptionHandler(UnauthorizedExportAccessException.class)
    public ResponseEntity<Map<String, String>> onUnauthorized(UnauthorizedExportAccessException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "FORBIDDEN", "message", e.getMessage()));
    }

    @ExceptionHandler(ExportLinkUnavailableException.class)
    public ResponseEntity<Map<String, String>> onUnavailable(ExportLinkUnavailableException e) {
        return ResponseEntity.status(HttpStatus.GONE)
                .body(Map.of("error", "EXPORT_LINK_UNAVAILABLE", "message", e.getMessage()));
    }

    private static String extensionFor(ExportFormat format) {
        return switch (format) {
            case JSON -> ".json";
            case PDF -> ".pdf";
            case BOTH -> ".bin";
        };
    }

    public record TriggerRequest(ExportFormat format) {
    }

    public record ExportSummary(
            UUID id,
            UUID childId,
            ExportFormat format,
            String token,
            String signedUrlPath,
            String createdAt,
            String expiresAt,
            int size) {

        public static ExportSummary from(ExportFile file) {
            return new ExportSummary(
                    file.id(),
                    file.childId(),
                    file.format(),
                    file.token(),
                    "/api/parents/me/exports/" + file.token(),
                    file.createdAt().toString(),
                    file.expiresAt().toString(),
                    file.contentSize());
        }
    }
}
