package ch.numnia.iam.config;

import ch.numnia.iam.api.dto.ErrorResponse;
import ch.numnia.iam.domain.AuditAction;
import ch.numnia.iam.domain.AuditLogEntry;
import ch.numnia.iam.domain.ChildProfile;
import ch.numnia.iam.domain.ChildSession;
import ch.numnia.iam.spi.AuditLogRepository;
import ch.numnia.iam.spi.ChildProfileRepository;
import ch.numnia.iam.spi.ChildSessionRepository;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring MVC interceptor that enforces cross-area authorization for child sessions.
 *
 * <p>UC-002 exception flow: "Attempt to navigate to parent/school areas from the child
 * session: the server denies with 403; audit-log entry."
 *
 * <p>When a valid {@code X-Numnia-Session} header is present and resolves to a CHILD-role
 * session, any request to the parent area ({@code GET /api/parents/me}) is blocked with
 * HTTP 403 and an {@code PARENT_ENDPOINT_DENIED_FOR_CHILD} audit entry is written.
 *
 * <p>Public registration/verification paths are not blocked, as they are invoked
 * anonymously (no session header present).
 *
 * <p>Privacy: childId and parentId are referenced by UUID only in audit log (NFR-PRIV-001).
 */
public class SessionInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(SessionInterceptor.class);

    /** Request attribute key for the resolved child session (used by controllers). */
    public static final String ATTR_CHILD_SESSION = "childSession";

    private final ChildSessionRepository sessionRepo;
    private final ChildProfileRepository childProfileRepo;
    private final AuditLogRepository auditLog;
    private final ObjectMapper objectMapper;

    public SessionInterceptor(ChildSessionRepository sessionRepo,
                              ChildProfileRepository childProfileRepo,
                              AuditLogRepository auditLog,
                              ObjectMapper objectMapper) {
        this.sessionRepo = sessionRepo;
        this.childProfileRepo = childProfileRepo;
        this.auditLog = auditLog;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws IOException {

        String tokenHeader = request.getHeader("X-Numnia-Session");
        if (tokenHeader == null) {
            return true; // no session — pass through (anonymous request)
        }

        UUID tokenId;
        try {
            tokenId = UUID.fromString(tokenHeader);
        } catch (IllegalArgumentException e) {
            return true; // malformed token — not our concern here, pass through
        }

        Optional<ChildSession> sessionOpt = sessionRepo.findById(tokenId);
        if (sessionOpt.isEmpty() || !sessionOpt.get().isValid()) {
            return true; // unknown or expired session — pass through
        }

        ChildSession session = sessionOpt.get();

        // Store session on request for downstream handlers
        request.setAttribute(ATTR_CHILD_SESSION, session);

        // Cross-area guard: CHILD sessions must not access parent-area endpoints (UC-002 exc. flow)
        if ("CHILD".equals(session.getRole()) && isParentAreaEndpoint(request)) {
            writeAuditEntry(session);
            writeErrorResponse(response);
            log.warn("Cross-area access blocked (childRef={}, path={})",
                    session.getChildId(), request.getRequestURI());
            return false;
        }

        return true;
    }

    /**
     * Returns {@code true} for endpoints that belong to the restricted parent area.
     *
     * <p>Current parent-area endpoints blocked for CHILD sessions:
     * <ul>
     *   <li>{@code GET /api/parents/me} — parent profile (placeholder; expanded in UC-009)
     * </ul>
     *
     * <p>Registration, verification, PIN management and lock-release paths are intentionally
     * not in this list, as they are invoked anonymously or by the parent before the child
     * signs in.
     */
    private boolean isParentAreaEndpoint(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        return "GET".equals(method) && path.startsWith("/api/parents/me");
    }

    private void writeAuditEntry(ChildSession session) {
        // Resolve pseudonym for audit log (no PII — pseudonym only, NFR-PRIV-001)
        String childRef = childProfileRepo.findById(session.getChildId())
                .map(ChildProfile::getPseudonym)
                .orElse(session.getChildId().toString());

        auditLog.save(new AuditLogEntry(
                AuditAction.PARENT_ENDPOINT_DENIED_FOR_CHILD,
                session.getParentId().toString(),
                childRef,
                null));
    }

    private void writeErrorResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
                new ErrorResponse("FORBIDDEN", "Child sessions may not access parent endpoints"));
    }
}
