package ch.numnia.learning.infra;

import ch.numnia.learning.domain.Operation;
import ch.numnia.learning.spi.TaskPoolRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory configurable task pool registry. Defaults to a sample world with
 * all four operations (FR-OPS-002). Replace with externalised configuration
 * (YAML or database) when UC-009 / UC-OPS lands.
 */
@Repository
public class InMemoryTaskPoolRepository implements TaskPoolRepository {

    public static final String DEFAULT_WORLD = "SAMPLE_WORLD";

    private final Map<String, Set<Operation>> pools = new ConcurrentHashMap<>();

    public InMemoryTaskPoolRepository() {
        pools.put(DEFAULT_WORLD, Set.of(Operation.values()));
    }

    @Override
    public boolean isConfigured(String worldId, Operation operation) {
        Set<Operation> ops = pools.get(worldId);
        return ops != null && ops.contains(operation);
    }

    public void clear(String worldId) { pools.remove(worldId); }

    public void reseedDefault() {
        pools.put(DEFAULT_WORLD, Set.of(Operation.values()));
    }
}
