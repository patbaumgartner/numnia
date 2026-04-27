package ch.numnia.worlds.infra;

import ch.numnia.worlds.domain.World;
import ch.numnia.worlds.spi.WorldCatalog;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Hardcoded R1 world catalogue (BR-001: exactly three openable worlds in
 * Release 1). Display names are Swiss High German with umlauts, without
 * sharp s (NFR-I18N-002, NFR-I18N-004).
 */
@Component
public class StaticWorldCatalog implements WorldCatalog {

    public static final String MUSHROOM_JUNGLE = "mushroom-jungle";
    public static final String CRYSTAL_CAVE = "crystal-cave";
    public static final String CLOUD_ISLAND = "cloud-island";

    private static final List<World> WORLDS = List.of(
            new World(MUSHROOM_JUNGLE, "Pilzdschungel", 1, 1),
            new World(CRYSTAL_CAVE, "Kristallhoehle", 2, 2),
            new World(CLOUD_ISLAND, "Wolkeninsel", 3, 3));

    @Override
    public List<World> listAll() {
        return WORLDS;
    }

    @Override
    public Optional<World> findById(String worldId) {
        return WORLDS.stream().filter(w -> w.id().equals(worldId)).findFirst();
    }
}
