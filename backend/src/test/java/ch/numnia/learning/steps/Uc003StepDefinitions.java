package ch.numnia.learning.steps;

import ch.numnia.learning.domain.*;
import ch.numnia.learning.infra.*;
import ch.numnia.learning.service.*;
import ch.numnia.learning.spi.LearningProgressRepository;
import ch.numnia.learning.spi.StarPointsRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cucumber step definitions for UC-003 — child training mode.
 *
 * <p>Wires through to the live Spring beans (in-memory repos by default) and
 * uses a fresh {@link UUID} child reference per scenario.
 */
public class Uc003StepDefinitions {

    @Autowired private TrainingService trainingService;
    @Autowired private LearningProgressRepository progressRepo;
    @Autowired private StarPointsRepository starPointsRepo;
    @Autowired private InMemoryTaskPoolRepository taskPools;
    @Autowired private MasteryTracker masteryTracker;
    @Autowired private TaskGenerator taskGenerator;

    private UUID childId;
    private TrainingSession session;
    private AnswerResult lastResult;
    private MathTask generatedTask;
    private LearningProgress progress;
    private boolean firstSessionThresholdsMet;

    @Before
    public void resetState() {
        childId = UUID.randomUUID();
        session = null;
        lastResult = null;
        generatedTask = null;
        progress = null;
        firstSessionThresholdsMet = false;
        taskPools.reseedDefault();
    }

    // ── Background ─────────────────────────────────────────────────────────
    // "Given an active child session" is provided by Uc002StepDefinitions
    // (it bootstraps a parent + child + PIN when invoked cold).

    @And("a configured task pool for the chosen world")
    public void aConfiguredTaskPool() {
        taskPools.reseedDefault();
    }

    // ── Scenario: speed downgrade ─────────────────────────────────────────

    @Given("the child practices multiplication on S3\\/G3")
    public void practiceMultiplicationS3G3() {
        progressRepo.save(new LearningProgress(childId, Operation.MULTIPLICATION, 3, 3));
        session = trainingService.startSession(childId, Operation.MULTIPLICATION,
                InMemoryTaskPoolRepository.DEFAULT_WORLD);
    }

    @When("it answers three tasks in a row wrong or by time-out")
    public void threeWrongOrTimeout() {
        for (int i = 0; i < 3; i++) {
            trainingService.nextTask(session.id());
            lastResult = trainingService.submitAnswer(session.id(), Integer.MIN_VALUE, 1000);
        }
    }

    @Then("the adaptive engine sets the speed to G2")
    public void speedSetToG2() {
        assertThat(lastResult.currentSpeed()).isEqualTo(2);
    }

    @And("proposes accuracy or explanation mode")
    public void proposesAccuracyOrExplanation() {
        assertThat(lastResult.modeSuggestion())
                .isIn(ModeSuggestion.ACCURACY, ModeSuggestion.EXPLANATION);
    }

    // ── Scenario: number range ────────────────────────────────────────────

    @Given("the child practices addition on S6")
    public void practiceAdditionS6() {
        progressRepo.save(new LearningProgress(childId, Operation.ADDITION, 6, 2));
        session = trainingService.startSession(childId, Operation.ADDITION,
                InMemoryTaskPoolRepository.DEFAULT_WORLD);
    }

    @When("the task generator creates a new task")
    public void generateTask() {
        generatedTask = trainingService.nextTask(session.id());
    }

    @Then("the expected result lies between 0 and 1,000,000")
    public void resultWithinRange() {
        assertThat(generatedTask.expectedAnswer()).isBetween(0, 1_000_000);
    }

    // ── Scenario: mastery consolidation ───────────────────────────────────

    @Given("the child meets the accuracy and speed thresholds for S2 today")
    public void childMeetsThresholdsToday() {
        progressRepo.save(new LearningProgress(childId, Operation.ADDITION, 2, 2));
        session = trainingService.startSession(childId, Operation.ADDITION,
                InMemoryTaskPoolRepository.DEFAULT_WORLD);
        for (int i = 0; i < 5; i++) {
            MathTask t = trainingService.nextTask(session.id());
            trainingService.submitAnswer(session.id(), t.expectedAnswer(), 500);
        }
        firstSessionThresholdsMet = true;
    }

    @And("only one session on one calendar day exists so far")
    public void onlyOneSession() {
        // implicit: only one session created in previous step.
    }

    @When("the session ends")
    public void sessionEnds() {
        SessionSummary summary = trainingService.endSession(session.id());
        progress = progressRepo.findByChildAndOperation(childId, Operation.ADDITION).orElseThrow();
        assertThat(summary.masteryStatus()).isEqualTo(MasteryStatus.IN_CONSOLIDATION);
    }

    @Then("the mastery status for S2 remains \"in consolidation\"")
    public void masteryRemainsInConsolidation() {
        assertThat(progress.masteryStatus()).isEqualTo(MasteryStatus.IN_CONSOLIDATION);
    }

    @And("mastery is confirmed only after a second session on another calendar day")
    public void masteryConfirmedOnLaterDay() {
        // simulate the next-day evaluation directly via tracker (clock-injected
        // service requires a separate context which is out of scope here).
        boolean promoted = masteryTracker.evaluate(progress, true,
                progress.firstQualifiedDate().plusDays(1));
        assertThat(promoted).isTrue();
        assertThat(progress.masteryStatus()).isEqualTo(MasteryStatus.MASTERED);
    }

    // ── Scenario: error costs no star points ──────────────────────────────

    @Given("the child has 12 star points")
    public void childHas12StarPoints() {
        starPointsRepo.setBalance(childId, 12);
        progressRepo.save(new LearningProgress(childId, Operation.ADDITION, 2, 2));
        session = trainingService.startSession(childId, Operation.ADDITION,
                InMemoryTaskPoolRepository.DEFAULT_WORLD);
        trainingService.nextTask(session.id());
    }

    @When("it answers a task wrong")
    public void answersTaskWrong() {
        lastResult = trainingService.submitAnswer(session.id(), Integer.MIN_VALUE, 500);
        assertThat(lastResult.outcome()).isEqualTo(AnswerOutcome.WRONG);
    }

    @Then("the star points balance stays at 12")
    public void starPointsBalanceUnchanged() {
        assertThat(lastResult.starPointsBalance()).isEqualTo(12);
        assertThat(starPointsRepo.balanceOf(childId)).isEqualTo(12);
    }
}
