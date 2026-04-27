package ch.numnia.learning.service;

import ch.numnia.learning.domain.LearningProgress;
import ch.numnia.learning.domain.MasteryStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Mastery promotion logic (BR-004, FR-LEARN-012).
 *
 * <p>Mastery is granted only after at least two qualifying sessions on two
 * distinct calendar days.
 */
@Component
public class MasteryTracker {

    public boolean evaluate(LearningProgress progress, boolean thresholdsMet, LocalDate today) {
        if (!thresholdsMet) {
            return false;
        }
        return switch (progress.masteryStatus()) {
            case NOT_STARTED -> {
                progress.setMasteryStatus(MasteryStatus.IN_CONSOLIDATION);
                progress.setFirstQualifiedDate(today);
                yield true;
            }
            case IN_CONSOLIDATION -> {
                LocalDate firstDay = progress.firstQualifiedDate();
                if (firstDay != null && today.isAfter(firstDay)) {
                    progress.setMasteryStatus(MasteryStatus.MASTERED);
                    yield true;
                }
                yield false;
            }
            case MASTERED -> false;
        };
    }
}
