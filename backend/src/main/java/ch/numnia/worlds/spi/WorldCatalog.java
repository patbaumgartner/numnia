package ch.numnia.worlds.spi;

import ch.numnia.worlds.domain.World;

import java.util.List;
import java.util.Optional;

/** Catalogue of available worlds (FR-WORLD-001). */
public interface WorldCatalog {

    List<World> listAll();

    Optional<World> findById(String worldId);
}
