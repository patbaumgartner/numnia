package ch.numnia.learning;

import ch.numnia.learning.domain.LearningProgress;
import ch.numnia.learning.domain.MasteryStatus;
import ch.numnia.learning.domain.Operation;
import ch.numnia.learning.service.MasteryTracker;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MasteryTrackerTest {

    private final MasteryTracker tracker = new MasteryTracker();
    private final UUID childId = UUID.randomUUID();

    @Test
    void evaluate_thresholdsMetFirstTime_movesToInConsolidationOnly() {
        var progress = new LearningProgress(childId, Operation.ADDITION, 2, 2);
        LocalDate today = LocalDate.of(2026, 5, 1);

        boolean changed = tracker.evaluate(progress, true, today);

        assertThat(changed).isTrue();
        assertThat(progress.masteryStatus()).isEqualTo(MasteryStatus.IN_CONSOLIDATION);
        assertThat(progress.firstQualifiedDate()).isEqualTo(today);
    }

    @Test
    void evaluate_secondQualifyingSessionSameDay_keepsInConsolidation() {
        var progress = new LearningProgress(childId, Operation.ADDITION, 2, 2);
        LocalDate today = LocalDate.of(2026, 5, 1);
        tracker.evaluate(progress, true, today);

        boolean changedAgain = tracker.evaluate(progress, true, today);

        assertThat(changedAgain).isFalse();
        assertThat(progress.masteryStatus()).isEqualTo(MasteryStatus.IN_CONSOLIDATION);
    }

    @Test
    void evaluate_secondQualifyingSessionLaterDay_promotesToMastered() {
        var progress = new LearningProgress(childId, Operation.ADDITION, 2, 2);
        tracker.evaluate(progress, true, LocalDate.of(2026, 5, 1));

        boolean changedAgain = tracker.evaluate(progress, true, LocalDate.of(2026, 5, 2));

        assertThat(changedAgain).isTrue();
        assertThat(progress.masteryStatus()).isEqualTo(MasteryStatus.MASTERED);
    }

    @Test
    void evaluate_notMet_doesNotChangeStatus() {
        var progress = new LearningProgress(childId, Operation.ADDITION, 2, 2);

        boolean changed = tracker.evaluate(progress, false, LocalDate.now());

        assertThat(changed).isFalse();
        assertThat(progress.masteryStatus()).isEqualTo(MasteryStatus.NOT_STARTED);
    }
}
