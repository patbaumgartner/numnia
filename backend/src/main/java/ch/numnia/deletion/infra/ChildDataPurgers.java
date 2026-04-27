package ch.numnia.deletion.infra;

import ch.numnia.avatar.spi.AvatarConfigurationRepository;
import ch.numnia.avatar.spi.InventoryRepository;
import ch.numnia.creatures.spi.CompanionRepository;
import ch.numnia.creatures.spi.CreatureInventoryRepository;
import ch.numnia.dataexport.spi.ExportFileRepository;
import ch.numnia.deletion.spi.ChildDataPurger;
import ch.numnia.learning.spi.LearningProgressRepository;
import ch.numnia.learning.spi.StarPointsRepository;
import ch.numnia.learning.spi.TrainingSessionRepository;
import ch.numnia.parentcontrols.spi.ChildControlsRepository;
import ch.numnia.parentcontrols.spi.RoundPoolRepository;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Collection of {@link ChildDataPurger} implementations for UC-011, one per
 * module that holds child-scoped data. Each purger is idempotent and returns
 * the data-category labels it actually touched (for the deletion record,
 * BR-002). Bundled here for discoverability — split per module if/when each
 * module gains its own purger logic beyond a single repository call.
 */
public final class ChildDataPurgers {

    private ChildDataPurgers() { }

    @Component
    public static class LearningProgressPurger implements ChildDataPurger {
        private final LearningProgressRepository learningProgress;
        public LearningProgressPurger(LearningProgressRepository learningProgress) {
            this.learningProgress = learningProgress;
        }
        @Override
        public Set<String> purge(UUID childId) {
            int n = learningProgress.deleteByChildId(childId);
            return n > 0 ? Set.of("learning-progress") : Set.of();
        }
    }

    @Component
    public static class TrainingSessionPurger implements ChildDataPurger {
        private final TrainingSessionRepository sessions;
        public TrainingSessionPurger(TrainingSessionRepository sessions) {
            this.sessions = sessions;
        }
        @Override
        public Set<String> purge(UUID childId) {
            int n = sessions.deleteByChildId(childId);
            return n > 0 ? Set.of("training-sessions") : Set.of();
        }
    }

    @Component
    public static class StarPointsPurger implements ChildDataPurger {
        private final StarPointsRepository starPoints;
        public StarPointsPurger(StarPointsRepository starPoints) {
            this.starPoints = starPoints;
        }
        @Override
        public Set<String> purge(UUID childId) {
            return starPoints.deleteByChildId(childId)
                    ? Set.of("star-points") : Set.of();
        }
    }

    @Component
    public static class InventoryPurger implements ChildDataPurger {
        private final InventoryRepository inventory;
        public InventoryPurger(InventoryRepository inventory) {
            this.inventory = inventory;
        }
        @Override
        public Set<String> purge(UUID childId) {
            int n = inventory.deleteByChildId(childId);
            return n > 0 ? Set.of("inventory") : Set.of();
        }
    }

    @Component
    public static class AvatarConfigurationPurger implements ChildDataPurger {
        private final AvatarConfigurationRepository config;
        public AvatarConfigurationPurger(AvatarConfigurationRepository config) {
            this.config = config;
        }
        @Override
        public Set<String> purge(UUID childId) {
            return config.deleteByChildId(childId)
                    ? Set.of("avatar-configuration") : Set.of();
        }
    }

    @Component
    public static class CreatureInventoryPurger implements ChildDataPurger {
        private final CreatureInventoryRepository creatures;
        public CreatureInventoryPurger(CreatureInventoryRepository creatures) {
            this.creatures = creatures;
        }
        @Override
        public Set<String> purge(UUID childId) {
            int n = creatures.deleteByChildId(childId);
            return n > 0 ? Set.of("creatures") : Set.of();
        }
    }

    @Component
    public static class CompanionPurger implements ChildDataPurger {
        private final CompanionRepository companions;
        public CompanionPurger(CompanionRepository companions) {
            this.companions = companions;
        }
        @Override
        public Set<String> purge(UUID childId) {
            return companions.deleteByChildId(childId)
                    ? Set.of("creatures") : Set.of();
        }
    }

    @Component
    public static class ExportFilePurger implements ChildDataPurger {
        private final ExportFileRepository exportFiles;
        public ExportFilePurger(ExportFileRepository exportFiles) {
            this.exportFiles = exportFiles;
        }
        @Override
        public Set<String> purge(UUID childId) {
            int n = exportFiles.deleteByChildId(childId);
            return n > 0 ? Set.of("exports") : Set.of();
        }
    }

    @Component
    public static class ChildControlsPurger implements ChildDataPurger {
        private final ChildControlsRepository controls;
        private final RoundPoolRepository roundPool;
        public ChildControlsPurger(ChildControlsRepository controls,
                                   RoundPoolRepository roundPool) {
            this.controls = controls;
            this.roundPool = roundPool;
        }
        @Override
        public Set<String> purge(UUID childId) {
            Set<String> touched = new LinkedHashSet<>();
            if (controls.deleteByChildId(childId)) {
                touched.add("child-controls");
            }
            roundPool.clear(childId);
            // round-pool is best-effort idempotent; do not advertise a category
            // unless controls existed (avoids false-positives in deletion record).
            return touched;
        }
    }
}
