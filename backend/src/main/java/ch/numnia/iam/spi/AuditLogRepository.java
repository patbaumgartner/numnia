package ch.numnia.iam.spi;

import ch.numnia.iam.domain.AuditAction;
import ch.numnia.iam.domain.AuditLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link AuditLogEntry}.
 *
 * <p>Query by {@code parentRef} (UUID string) only — never by email, ensuring
 * no PII leaks into query parameters or logs (NFR-PRIV-001).
 */
public interface AuditLogRepository extends JpaRepository<AuditLogEntry, Long> {

    List<AuditLogEntry> findAllByParentRefOrderByTimestampAsc(String parentRef);

    boolean existsByParentRefAndAction(String parentRef, AuditAction action);
}
