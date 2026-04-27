package ch.numnia.deletion.infra;

import ch.numnia.deletion.domain.DeletionRequest;
import ch.numnia.deletion.domain.DeletionStatus;
import ch.numnia.deletion.spi.DeletionRequestRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Volatile in-memory store; will move to Postgres + Flyway. */
@Repository
public class InMemoryDeletionRequestRepository implements DeletionRequestRepository {

    private final Map<UUID, DeletionRequest> byId = new ConcurrentHashMap<>();
    private final Map<String, UUID> tokenIndex = new ConcurrentHashMap<>();

    @Override
    public void save(DeletionRequest request) {
        byId.put(request.id(), request);
        tokenIndex.put(request.token(), request.id());
    }

    @Override
    public Optional<DeletionRequest> findByToken(String token) {
        if (token == null) return Optional.empty();
        UUID id = tokenIndex.get(token);
        if (id == null) return Optional.empty();
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<DeletionRequest> findById(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public List<DeletionRequest> findAllByParentId(UUID parentId) {
        List<DeletionRequest> result = new ArrayList<>();
        for (DeletionRequest r : byId.values()) {
            if (r.parentId().equals(parentId)) {
                result.add(r);
            }
        }
        return result;
    }

    @Override
    public List<DeletionRequest> findAllByStatus(DeletionStatus status) {
        List<DeletionRequest> result = new ArrayList<>();
        for (DeletionRequest r : byId.values()) {
            if (r.status() == status) {
                result.add(r);
            }
        }
        return result;
    }

    @Override
    public void deleteAll() {
        byId.clear();
        tokenIndex.clear();
    }
}
