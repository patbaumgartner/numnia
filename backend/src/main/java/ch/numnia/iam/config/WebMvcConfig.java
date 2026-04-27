package ch.numnia.iam.config;

import ch.numnia.iam.spi.AuditLogRepository;
import ch.numnia.iam.spi.ChildProfileRepository;
import ch.numnia.iam.spi.ChildSessionRepository;
import tools.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC configuration for the IAM module.
 *
 * <p>Registers the {@link SessionInterceptor} that enforces cross-area
 * authorization for CHILD sessions (UC-002 exception flow).
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ChildSessionRepository sessionRepo;
    private final ChildProfileRepository childProfileRepo;
    private final AuditLogRepository auditLog;
    private final ObjectMapper objectMapper;

    public WebMvcConfig(ChildSessionRepository sessionRepo,
                        ChildProfileRepository childProfileRepo,
                        AuditLogRepository auditLog,
                        ObjectMapper objectMapper) {
        this.sessionRepo = sessionRepo;
        this.childProfileRepo = childProfileRepo;
        this.auditLog = auditLog;
        this.objectMapper = objectMapper;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(
                new SessionInterceptor(sessionRepo, childProfileRepo, auditLog, objectMapper));
    }
}
