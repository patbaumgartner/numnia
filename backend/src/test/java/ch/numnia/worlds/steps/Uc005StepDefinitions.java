package ch.numnia.worlds.steps;

import ch.numnia.learning.domain.LearningProgress;
import ch.numnia.learning.domain.Operation;
import ch.numnia.learning.infra.InMemoryTaskPoolRepository;
import ch.numnia.learning.spi.LearningProgressRepository;
import ch.numnia.worlds.domain.PortalEntry;
import ch.numnia.worlds.domain.PortalType;
import ch.numnia.worlds.domain.WorldAuditAction;
import ch.numnia.worlds.infra.StaticWorldCatalog;
import ch.numnia.worlds.service.WorldService;
import ch.numnia.worlds.spi.ChildPreferencesRepository;
import ch.numnia.worlds.spi.WorldAuditRepository;
import ch.numnia.worlds.spi.WorldCatalog;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cucumber step definitions for UC-005 — child enters a world through a portal.
 *
 * <p>Wires through to live Spring beans (in-memory repos). The shared
 * {@code Given an active child session} step is contributed by
 * {@code Uc002StepDefinitions}; this class uses its own per-scenario
 * {@link UUID} child reference, identical to the UC-003 pattern.
 */
public class Uc005StepDefinitions {

    @Autowired private WorldService worldService;
    @Autowired private WorldCatalog worldCatalog;
    @Autowired private ChildPreferencesRepository preferences;
    @Autowired private WorldAuditRepository worldAudit;
    @Autowired private LearningProgressRepository progressRepo;
    @Autowired private InMemoryTaskPoolRepository taskPools;

    private UUID childId;
    private PortalEntry lastEntry;
    private PortalType portalUnderTest;

    @Before
    public void resetState() {
        childId = UUID.randomUUID();
        lastEntry = null;
        portalUnderTest = null;
        taskPools.reseedDefault();
    }

    @And("three worlds are unlocked in Release 1")
    public void threeWorldsUnlocked() {
        assertThat(worldCatalog.listAll()).hasSize(3);
    }

    // ── Scenario: Training portal opens when rules are satisfied ──────────

    @Given("the child has reached level S2")
    public void childHasReachedLevelS2() {
        progressRepo.save(new LearningProgress(childId, Operation.ADDITION, 2, 1));
    }

    @And("the task pool of the world {string} is configured")
    public void taskPoolConfigured(String worldName) {
        // Reseed via the in-memory repository — the three R1 slugs are seeded
        // by default in InMemoryTaskPoolRepository.
        taskPools.reseedDefault();
        assertThat(taskPools.isConfigured(StaticWorldCatalog.MUSHROOM_JUNGLE, Operation.ADDITION))
                .isTrue();
    }

    @When("the child enters the training portal of Mushroom Jungle")
    public void enterTrainingPortalMushroomJungle() {
        lastEntry = worldService.openPortal(
                childId, StaticWorldCatalog.MUSHROOM_JUNGLE, PortalType.TRAINING);
    }

    @Then("the system switches to the practice stage of the world")
    public void systemSwitchesToPracticeStage() {
        assertThat(lastEntry.locked()).isFalse();
        assertThat(lastEntry.target()).isEqualTo(PortalEntry.TARGET_PRACTICE_STAGE);
        assertThat(worldAudit.actionsFor(childId)).contains(WorldAuditAction.PORTAL_OPENED);
    }

    // ── Scenario: Reduced-motion reduces animations ────────────────────────

    @Given("the child has reduced-motion enabled")
    public void reducedMotionEnabled() {
        preferences.setReducedMotion(childId, true);
    }

    @When("it enters a world")
    public void itEntersAWorld() {
        progressRepo.save(new LearningProgress(childId, Operation.ADDITION, 1, 1));
        lastEntry = worldService.openPortal(
                childId, StaticWorldCatalog.MUSHROOM_JUNGLE, PortalType.TRAINING);
    }

    @Then("particle effects and intense animations are reduced")
    public void particleEffectsReduced() {
        assertThat(lastEntry.reducedMotion()).isTrue();
        assertThat(worldAudit.actionsFor(childId))
                .contains(WorldAuditAction.REDUCED_MOTION_APPLIED);
    }

    // ── Scenario: Locked portal stays closed ───────────────────────────────

    @Given("a portal of type {string}")
    public void portalOfType(String type) {
        portalUnderTest = PortalType.valueOf(type.toUpperCase());
    }

    @When("the child taps on it in Release 1")
    public void childTapsOnPortal() {
        lastEntry = worldService.openPortal(
                childId, StaticWorldCatalog.MUSHROOM_JUNGLE, portalUnderTest);
    }

    @Then("the system shows the notice {string}")
    public void systemShowsNotice(String notice) {
        assertThat(notice).isEqualTo("coming later");
        assertThat(lastEntry.messageCode()).isEqualTo(PortalEntry.CODE_COMING_LATER);
    }

    @And("the portal stays closed")
    public void portalStaysClosed() {
        assertThat(lastEntry.locked()).isTrue();
        assertThat(lastEntry.target()).isNull();
        assertThat(worldAudit.actionsFor(childId))
                .contains(WorldAuditAction.PORTAL_LOCKED_RELEASE_RULE);
    }
}
