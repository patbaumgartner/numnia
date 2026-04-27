package ch.numnia.deletion.spi;

import ch.numnia.deletion.domain.DeletionRequest;
import ch.numnia.deletion.domain.DeletionStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence boundary for {@link DeletionRequest} (UC-011). */
public interface DeletionRequestRepository {

    void save(DeletionRequest request);

    Optional<DeletionRequest> findByToken(String token);

    Optional<DeletionRequest> findById(UUID id);

    List<DeletionRequest> findAllByParentId(UUID parentId);

    List<DeletionRequest> findAllByStatus(DeletionStatus status);

    void deleteAll();
}
