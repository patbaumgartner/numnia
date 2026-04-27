package ch.numnia.iam.spi;

import ch.numnia.iam.domain.ParentAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ParentAccount}.
 *
 * <p>Swapping to a full Postgres + Flyway setup is mechanical: replace the H2
 * test datasource with Testcontainers Postgres (deferred to UC-009/UC-011).
 */
public interface ParentAccountRepository extends JpaRepository<ParentAccount, UUID> {

    Optional<ParentAccount> findByEmail(String email);

    boolean existsByEmail(String email);
}
