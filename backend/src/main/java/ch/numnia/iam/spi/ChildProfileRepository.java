package ch.numnia.iam.spi;

import ch.numnia.iam.domain.ChildProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ChildProfile}.
 */
public interface ChildProfileRepository extends JpaRepository<ChildProfile, UUID> {

    List<ChildProfile> findAllByParentId(UUID parentId);

    long countByParentId(UUID parentId);
}
