package ch.numnia.learning.spi;

import ch.numnia.learning.domain.Operation;

/** Task pool configuration per (world, operation) — FR-OPS-002. */
public interface TaskPoolRepository {
    boolean isConfigured(String worldId, Operation operation);
}
