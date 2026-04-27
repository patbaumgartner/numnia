package ch.numnia.learning.steps;

import ch.numnia.learning.domain.*;
import ch.numnia.learning.infra.InMemoryTaskPoolRepository;
import ch.numnia.learning.service.ExplanationSteps;
import ch.numnia.learning.service.TrainingService;
import ch.numnia.learning.spi.StarPointsRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cucumber step definitions for UC-004 — accuracy-mode practice without
 * time pressure.
 *
 * <p>Background "Given an active child session" is provided by
 * {@code Uc002StepDefinitions} (self-bootstrapping).
 */
public class Uc004StepDefinitions {

    @Autowired private TrainingService trainingService;
    @Autowired private StarPointsRepository starPointsRepo;
    @Autowired private InMemoryTaskPoolRepository taskPools;

    private UUID childId;
    private TrainingSession session;
    private MathTask shownTask;
    private AnswerResult lastResult;
    private ExplanationSteps explanation;

    @Before
    public void resetState() {
        childId = UUID.randomUUID();
        session = null;
        shownTask = null;
        lastResult = null;
        explanation = null;
        taskPools.reseedDefault();
    }

    // ── Scenario 1: accuracy mode runs without a timer ────────────────────

    @Given("the child starts accuracy mode for subtraction")
    public void childStartsAccuracyForSubtraction() {
        session = trainingService.startAccuracySession(
                childId, Operation.SUBTRACTION, InMemoryTaskPoolRepository.DEFAULT_WORLD);
    }

    @When("a task is shown")
    public void aTaskIsShown() {
        shownTask = trainingService.nextTask(session.id());
    }

    @Then("no time limit is active")
    public void noTimeLimitIsActive() {
        assertThat(session.accuracyMode()).isTrue();
        assertThat(session.currentSpeed()).isZero();
    }

    @And("no timer is visible in the UI")
    public void noTimerVisibleInUi() {
        // Server-side guarantee surfaced to the UI: tasks emitted in accuracy
        // mode carry speed=0 (G0). The frontend renders no timer when
        // task.timed === false (asserted in AccuracyPage.test.tsx).
        assertThat(shownTask.speed()).isZero();
        assertThat(shownTask.timed()).isFalse();
    }

    // ── Scenario 2: explanation mode reachable from accuracy mode ─────────

    @Given("a task is shown in accuracy mode")
    public void taskShownInAccuracyMode() {
        session = trainingService.startAccuracySession(
                childId, Operation.ADDITION, InMemoryTaskPoolRepository.DEFAULT_WORLD);
        shownTask = trainingService.nextTask(session.id());
    }

    @When("the child selects {string}")
    public void childSelects(String action) {
        if ("Show explanation".equals(action)) {
            explanation = trainingService.getExplanation(session.id());
        } else {
            throw new IllegalArgumentException("Unsupported action: " + action);
        }
    }

    @Then("animated solution steps are played")
    public void animatedSolutionStepsArePlayed() {
        assertThat(explanation).isNotNull();
        assertThat(explanation.steps()).isNotEmpty();
    }

    @And("the task remains workable")
    public void taskRemainsWorkable() {
        // The current task must still be the one shown before the explanation
        // request; the child can answer it after watching the steps.
        assertThat(session.currentTask()).isEqualTo(shownTask);
    }

    // ── Scenario 3: no star point loss on error in accuracy mode ──────────

    @Given("a child with 8 star points")
    public void childWith8StarPoints() {
        starPointsRepo.setBalance(childId, 8);
    }

    @When("it answers a task wrong in accuracy mode")
    public void answersWrongInAccuracyMode() {
        session = trainingService.startAccuracySession(
                childId, Operation.ADDITION, InMemoryTaskPoolRepository.DEFAULT_WORLD);
        trainingService.nextTask(session.id());
        lastResult = trainingService.submitAnswer(session.id(), Integer.MIN_VALUE, 0L);
        assertThat(lastResult.outcome()).isEqualTo(AnswerOutcome.WRONG);
    }

    @Then("the star points balance stays at 8")
    public void starPointsBalanceStaysAt8() {
        assertThat(lastResult.starPointsBalance()).isEqualTo(8);
        assertThat(starPointsRepo.balanceOf(childId)).isEqualTo(8);
    }
}
