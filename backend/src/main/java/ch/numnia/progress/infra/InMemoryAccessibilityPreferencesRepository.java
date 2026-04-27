package ch.numnia.progress.infra;

import ch.numnia.progress.domain.ColorPalette;
import ch.numnia.progress.spi.AccessibilityPreferencesRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryAccessibilityPreferencesRepository implements AccessibilityPreferencesRepository {

    private final ConcurrentMap<UUID, ColorPalette> palettes = new ConcurrentHashMap<>();

    @Override
    public ColorPalette getPalette(UUID childId) {
        return palettes.getOrDefault(childId, ColorPalette.DEFAULT);
    }

    @Override
    public void setPalette(UUID childId, ColorPalette palette) {
        if (palette == null) {
            throw new IllegalArgumentException("palette must not be null");
        }
        palettes.put(childId, palette);
    }
}
