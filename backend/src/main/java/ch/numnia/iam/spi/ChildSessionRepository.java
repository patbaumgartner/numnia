package ch.numnia.iam.spi;

import ch.numnia.iam.domain.ChildSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ChildSession}.
 *
 * <p>Sessions are looked up by their UUID token value (the primary key),
 * which the client sends as the {@code X-Numnia-Session} header.
 */
public interface ChildSessionRepository extends JpaRepository<ChildSession, UUID> {

    /**
     * Finds the active (non-revoked) session for a given child profile.
     * Used when signing out by child profile ID.
     */
    Optional<ChildSession> findFirstByChildIdAndRevokedAtIsNullOrderByCreatedAtDesc(UUID childId);
}
