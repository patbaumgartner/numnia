package ch.numnia.progress.service;

import ch.numnia.learning.domain.LearningProgress;
import ch.numnia.learning.domain.MasteryStatus;
import ch.numnia.learning.domain.Operation;
import ch.numnia.learning.domain.TrainingSession;
import ch.numnia.learning.spi.LearningProgressRepository;
import ch.numnia.learning.spi.TrainingSessionRepository;
import ch.numnia.progress.domain.ColorPalette;
import ch.numnia.progress.domain.OperationProgress;
import ch.numnia.progress.domain.ProgressOverview;
import ch.numnia.progress.spi.AccessibilityPreferencesRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aggregates per-operation learning progress for the child progress view (UC-008).
 *
 * <p>BR-001: only data of the queried child is exposed (queries are scoped
 * by {@code childId} and never aggregate across children).
 *
 * <p>BR-002: the result contains no comparative leaderboard fields — the
 * domain types intentionally do not model rankings.
 *
 * <p>NFR-PRIV-001: this service does not log per-call telemetry of the
 * progress request (UC-008 main flow step 4).
 */
@Service
public class ProgressService {

    private final LearningProgressRepository progressRepo;
    private final TrainingSessionRepository sessionRepo;
    private final AccessibilityPreferencesRepository accessibilityRepo;

    public ProgressService(LearningProgressRepository progressRepo,
                           TrainingSessionRepository sessionRepo,
                           AccessibilityPreferencesRepository accessibilityRepo) {
        this.progressRepo = progressRepo;
        this.sessionRepo = sessionRepo;
        this.accessibilityRepo = accessibilityRepo;
    }

    /**
     * Build the progress overview for one child. Operations without any
     * progress entry yet are omitted (alt-flow 1a is detected via
     * {@link ProgressOverview#empty()}).
     */
    public ProgressOverview getOverview(UUID childId) {
        if (childId == null) {
            throw new IllegalArgumentException("childId must not be null");
        }
        List<OperationProgress> entries = new ArrayList<>();
        for (Operation op : Operation.values()) {
            progressRepo.findByChildAndOperation(childId, op)
                    .map(p -> aggregate(childId, p))
                    .ifPresent(entries::add);
        }
        ColorPalette palette = accessibilityRepo.getPalette(childId);
        boolean empty = entries.stream().allMatch(e -> e.totalSessions() == 0);
        return new ProgressOverview(childId, palette, entries, empty);
    }

    /** UC-008 alt-flow 3a — switch the color palette for the visualization. */
    public void setPalette(UUID childId, ColorPalette palette) {
        if (childId == null) {
            throw new IllegalArgumentException("childId must not be null");
        }
        if (palette == null) {
            throw new IllegalArgumentException("palette must not be null");
        }
        accessibilityRepo.setPalette(childId, palette);
    }

    private OperationProgress aggregate(UUID childId, LearningProgress progress) {
        List<TrainingSession> ended = sessionRepo.findEndedByChildAndOperation(
                childId, progress.operation());
        int totalSessions = ended.size();
        int totalTasks = ended.stream().mapToInt(TrainingSession::totalTasks).sum();
        int correctTasks = ended.stream().mapToInt(TrainingSession::correctTasks).sum();
        double accuracy = totalTasks == 0 ? 0.0 : (double) correctTasks / totalTasks;
        MasteryStatus status = progress.masteryStatus();
        return new OperationProgress(
                progress.operation(),
                totalSessions,
                totalTasks,
                correctTasks,
                accuracy,
                status,
                progress.currentDifficulty());
    }
}
