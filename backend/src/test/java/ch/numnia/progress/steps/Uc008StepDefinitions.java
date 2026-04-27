package ch.numnia.progress.steps;

import ch.numnia.learning.domain.LearningProgress;
import ch.numnia.learning.domain.MasteryStatus;
import ch.numnia.learning.domain.Operation;
import ch.numnia.learning.infra.InMemoryTaskPoolRepository;
import ch.numnia.learning.service.SessionSummary;
import ch.numnia.learning.service.TrainingService;
import ch.numnia.learning.spi.LearningProgressRepository;
import ch.numnia.progress.domain.ColorPalette;
import ch.numnia.progress.domain.OperationProgress;
import ch.numnia.progress.domain.ProgressOverview;
import ch.numnia.progress.service.ProgressService;
import ch.numnia.test.TestScenarioContext;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cucumber step definitions for UC-008 — child views own learning progress.
 */
public class Uc008StepDefinitions {

    @Autowired private ProgressService progressService;
    @Autowired private TrainingService trainingService;
    @Autowired private LearningProgressRepository progressRepo;
    @Autowired private InMemoryTaskPoolRepository taskPools;
    @Autowired private TestScenarioContext scenarioContext;

    private UUID childId;
    private ProgressOverview overview;

    @Before
    public void resetState() {
        overview = null;
        childId = null;
        taskPools.reseedDefault();
    }

    private UUID childId() {
        if (childId != null) return childId;
        childId = scenarioContext.childId();
        if (childId == null) {
            childId = UUID.randomUUID();
            scenarioContext.setChildId(childId);
        }
        return childId;
    }

    // ── Scenario 1: Progress per operation is visible ──────────────────────

    @Given("at least three completed training sessions")
    public void threeCompletedSessions() {
        UUID id = childId();
        // Seed an addition progress entry, then run three quick sessions to completion.
        progressRepo.save(new LearningProgress(id, Operation.ADDITION, 1, 2));
        for (int i = 0; i < 3; i++) {
            var session = trainingService.startSession(id, Operation.ADDITION,
                    InMemoryTaskPoolRepository.DEFAULT_WORLD);
            trainingService.nextTask(session.id());
            trainingService.submitAnswer(session.id(), Integer.MIN_VALUE, 1000);
            SessionSummary unused = trainingService.endSession(session.id());
            assertThat(unused).isNotNull();
        }
        // Mark addition as IN_CONSOLIDATION so the mastery marker is visible.
        var p = progressRepo.findByChildAndOperation(id, Operation.ADDITION).orElseThrow();
        p.setMasteryStatus(MasteryStatus.IN_CONSOLIDATION);
        progressRepo.save(p);
    }

    @When("the child opens \"My progress\"")
    public void childOpensMyProgress() {
        overview = progressService.getOverview(childId());
    }

    @Then("it sees a separate progress bar per operation")
    public void seesProgressBarPerOperation() {
        assertThat(overview).isNotNull();
        assertThat(overview.entries()).isNotEmpty();
        // Each entry corresponds to exactly one operation (a "progress bar" per op).
        assertThat(overview.entries()).extracting(OperationProgress::operation)
                .doesNotHaveDuplicates();
    }

    @And("the mastery status per content domain is marked")
    public void masteryStatusIsMarked() {
        assertThat(overview.entries())
                .allSatisfy(e -> assertThat(e.masteryStatus()).isNotNull());
        assertThat(overview.entries())
                .anyMatch(e -> e.masteryStatus() == MasteryStatus.IN_CONSOLIDATION
                        || e.masteryStatus() == MasteryStatus.MASTERED
                        || e.masteryStatus() == MasteryStatus.NOT_STARTED);
    }

    // ── Scenario 2: No comparative leaderboards ────────────────────────────

    @Given("the progress view is open")
    public void progressViewOpen() {
        // Seed minimal data and open the view.
        progressRepo.save(new LearningProgress(childId(), Operation.ADDITION, 1, 2));
        overview = progressService.getOverview(childId());
    }

    @Then("the view contains no leaderboard with other children")
    public void noLeaderboardWithOtherChildren() {
        // Domain types intentionally do not model leaderboards (BR-002).
        var overviewFields = ProgressOverview.class.getRecordComponents();
        var entryFields = OperationProgress.class.getRecordComponents();
        assertThat(overviewFields).extracting(java.lang.reflect.RecordComponent::getName)
                .doesNotContain("rank", "leaderboard", "peerAverage", "globalRanking");
        assertThat(entryFields).extracting(java.lang.reflect.RecordComponent::getName)
                .doesNotContain("rank", "leaderboard", "peerAverage", "globalRanking");
    }

    // ── Scenario 3: Color-blind profile is respected ───────────────────────

    @Given("the child has the deuteranopia profile enabled")
    public void deuteranopiaProfile() {
        progressService.setPalette(childId(), ColorPalette.DEUTERANOPIA);
    }

    @When("it opens the progress view")
    public void itOpensTheProgressView() {
        overview = progressService.getOverview(childId());
    }

    @Then("the visualization uses the corresponding color palette")
    public void visualizationUsesCorrespondingPalette() {
        assertThat(overview.palette()).isEqualTo(ColorPalette.DEUTERANOPIA);
    }
}
