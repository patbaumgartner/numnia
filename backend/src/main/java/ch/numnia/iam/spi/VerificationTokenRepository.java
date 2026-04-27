package ch.numnia.iam.spi;

import ch.numnia.iam.domain.TokenPurpose;
import ch.numnia.iam.domain.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link VerificationToken}.
 *
 * <p>Tokens are single-use and expire after 24 h. The {@code consumedAt IS NULL}
 * guard prevents reuse of consumed tokens.
 */
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

    /**
     * Returns the most recent unconsumed token for a parent and purpose.
     * Used by services to validate opt-in links.
     */
    Optional<VerificationToken> findFirstByParentIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
            UUID parentId, TokenPurpose purpose);

    /**
     * Returns all tokens for a parent — used by the e2e test helper.
     */
    List<VerificationToken> findAllByParentIdOrderByCreatedAtDesc(UUID parentId);
}
