package ch.numnia.avatar.domain;

import java.util.Map;
import java.util.UUID;

/**
 * Snapshot of a child's avatar configuration (UC-007).
 *
 * <p>BR-004: {@code baseModel} is one of the gender-neutral base models
 * configured in {@code IamConfig#avatarBaseModelCatalog()}.
 *
 * @param childId      child whose avatar this is
 * @param baseModel    chosen gender-neutral base model id
 * @param equipped     map of slot → equipped item id (may be empty)
 */
public record AvatarConfiguration(
        UUID childId,
        String baseModel,
        Map<String, String> equipped) {

    public AvatarConfiguration {
        if (childId == null) {
            throw new IllegalArgumentException("childId must not be null");
        }
        if (baseModel == null || baseModel.isBlank()) {
            throw new IllegalArgumentException("baseModel must not be blank");
        }
        equipped = equipped == null ? Map.of() : Map.copyOf(equipped);
    }
}
