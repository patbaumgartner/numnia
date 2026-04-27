package ch.numnia.avatar.spi;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Persists the equipped-items map per child (UC-007 step 6).
 *
 * <p>The base model itself lives on the {@code ChildProfile} (UC-001).
 */
public interface AvatarConfigurationRepository {

    Map<String, String> equippedFor(UUID childId);

    void equip(UUID childId, String slot, String itemId);

    Optional<String> equippedAt(UUID childId, String slot);

    /** Removes the avatar configuration for the given child (UC-011). */
    boolean deleteByChildId(UUID childId);
}
