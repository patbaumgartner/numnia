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

    /** Stable slugs of the three Release-1 worlds; kept in sync with {@code StaticWorldCatalog}. */
    public static final String R1_MUSHROOM_JUNGLE = "mushroom-jungle";
    public static final String R1_CRYSTAL_CAVE = "crystal-cave";
    public static final String R1_CLOUD_ISLAND = "cloud-island";

    private final Map<String, Set<Operation>> pools = new ConcurrentHashMap<>();

    public InMemoryTaskPoolRepository() {
        pools.put(DEFAULT_WORLD, Set.of(Operation.values()));
        pools.put(R1_MUSHROOM_JUNGLE, Set.of(Operation.values()));
        pools.put(R1_CRYSTAL_CAVE, Set.of(Operation.values()));
        pools.put(R1_CLOUD_ISLAND, Set.of(Operation.values()));
    }

    @Override
    public boolean isConfigured(String worldId, Operation operation) {
        Set<Operation> ops = pools.get(worldId);
        return ops != null && ops.contains(operation);
    }

    public void clear(String worldId) { pools.remove(worldId); }

    public void reseedDefault() {
        pools.put(DEFAULT_WORLD, Set.of(Operation.values()));
        pools.put(R1_MUSHROOM_JUNGLE, Set.of(Operation.values()));
        pools.put(R1_CRYSTAL_CAVE, Set.of(Operation.values()));
        pools.put(R1_CLOUD_ISLAND, Set.of(Operation.values()));
    }
}
