package ch.numnia.creatures.steps;

import ch.numnia.creatures.domain.Creature;
import ch.numnia.creatures.domain.CreatureAuditAction;
import ch.numnia.creatures.domain.CreatureUnlock;
import ch.numnia.creatures.domain.CreatureUnlockResult;
import ch.numnia.creatures.domain.GalleryEntry;
import ch.numnia.creatures.infra.StaticCreatureCatalog;
import ch.numnia.creatures.service.CompanionNotUnlockedException;
import ch.numnia.creatures.service.CreatureService;
import ch.numnia.creatures.spi.CompanionRepository;
import ch.numnia.creatures.spi.CreatureAuditRepository;
import ch.numnia.creatures.spi.CreatureCatalog;
import ch.numnia.creatures.spi.CreatureInventoryRepository;
import ch.numnia.learning.domain.LearningProgress;
import ch.numnia.learning.domain.MasteryStatus;
import ch.numnia.learning.domain.Operation;
import ch.numnia.learning.spi.LearningProgressRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cucumber step definitions for UC-006 — child unlocks a creature and picks
 * it as companion. The shared "Given an active child session" step is
 * contributed by {@code Uc002StepDefinitions}.
 */
public class Uc006StepDefinitions {

    @Autowired private CreatureService creatureService;
    @Autowired private CreatureCatalog creatureCatalog;
    @Autowired private CreatureInventoryRepository inventory;
    @Autowired private CompanionRepository companions;
    @Autowired private CreatureAuditRepository creatureAudit;
    @Autowired private LearningProgressRepository progressRepo;

    private UUID childId;
    private CreatureUnlockResult lastResult;
    private List<String> validationCandidates;
    private Throwable companionError;
    private String previousCompanion;

    @Before
    public void resetState() {
        childId = UUID.randomUUID();
        lastResult = null;
        validationCandidates = null;
        companionError = null;
        previousCompanion = null;
    }

    @And("a configured unlock threshold for the creature {string}")
    public void configuredThresholdFor(String creatureName) {
        Creature creature = creatureCatalog.findById(StaticCreatureCatalog.PILZAR_ID).orElseThrow();
        assertThat(creature.displayName()).isEqualTo(creatureName);
    }

    // ── Scenario: Successful unlock via mastery ───────────────────────────

    @Given("the child reaches mastery in the related domain")
    public void childReachesMastery() {
        LearningProgress progress = new LearningProgress(childId, Operation.ADDITION, 3, 3);
        progress.setMasteryStatus(MasteryStatus.MASTERED);
        progressRepo.save(progress);
    }

    @When("the system processes the unlock")
    public void systemProcessesUnlock() {
        lastResult = creatureService.processUnlocks(childId);
    }

    @Then("the creature appears permanently in the gallery")
    public void creatureAppearsInGallery() {
        assertThat(lastResult.newlyUnlocked())
                .extracting(Creature::id)
                .contains(StaticCreatureCatalog.PILZAR_ID);
        assertThat(inventory.unlockedCreatureIds(childId))
                .contains(StaticCreatureCatalog.PILZAR_ID);
        assertThat(creatureAudit.actionsFor(childId))
                .contains(CreatureAuditAction.CREATURE_UNLOCKED);
        // BR-001: re-running processUnlocks does not remove or duplicate entries.
        creatureService.processUnlocks(childId);
        assertThat(inventory.unlockedCreatureIds(childId))
                .containsOnlyOnce(StaticCreatureCatalog.PILZAR_ID);
    }

    @And("it can be picked as companion")
    public void canBePickedAsCompanion() {
        creatureService.pickCompanion(childId, StaticCreatureCatalog.PILZAR_ID);
        assertThat(companions.findCompanion(childId))
                .contains(StaticCreatureCatalog.PILZAR_ID);
        assertThat(creatureAudit.actionsFor(childId))
                .contains(CreatureAuditAction.COMPANION_CHANGED);
    }

    // ── Scenario: Variable name endings are accepted ──────────────────────

    @Given("the candidate names {string}, {string}, {string}")
    public void candidateNames(String a, String b, String c) {
        validationCandidates = Arrays.asList(a, b, c);
    }

    @When("the system validates the names")
    public void systemValidatesNames() {
        // Names are validated through the Creature record constructor (BR-002).
        // Build throwaway creatures using a benign world id.
        for (String name : validationCandidates) {
            new Creature(name.toLowerCase(), name, Operation.ADDITION, "mushroom-jungle");
        }
    }

    @Then("all three are accepted")
    public void allThreeAccepted() {
        assertThat(validationCandidates).hasSize(3);
        // No exception means all accepted; double-check different endings.
        assertThat(validationCandidates).extracting(s -> s.charAt(s.length() - 1))
                .containsExactlyInAnyOrder('r', 'o', 'a');
    }

    @And("the system rejects no name due to a missing {string} ending")
    public void rejectsNoNameDueToMissingEnding(String forbiddenEnding) {
        // BR-002: the name validation must not enforce any specific ending.
        // Verify the validator does not reject a name lacking the given suffix.
        for (String name : validationCandidates) {
            if (!name.endsWith(forbiddenEnding)) {
                new Creature(name.toLowerCase(), name, Operation.ADDITION, "mushroom-jungle");
            }
        }
    }

    // ── Scenario: Picking a non-unlocked creature is rejected ─────────────

    @Given("a creature that is not yet unlocked")
    public void aCreatureThatIsNotYetUnlocked() {
        // Establish a previous companion (Pilzar) by unlocking it first.
        inventory.recordUnlock(new CreatureUnlock(childId, StaticCreatureCatalog.PILZAR_ID, Instant.now()));
        creatureService.pickCompanion(childId, StaticCreatureCatalog.PILZAR_ID);
        previousCompanion = StaticCreatureCatalog.PILZAR_ID;
        // Welleno remains locked.
        assertThat(inventory.unlockedSet(childId))
                .doesNotContain(StaticCreatureCatalog.WELLENO_ID);
    }

    @When("the child tries to pick it as companion")
    public void childTriesToPickLockedCreature() {
        try {
            creatureService.pickCompanion(childId, StaticCreatureCatalog.WELLENO_ID);
        } catch (Throwable t) {
            companionError = t;
        }
    }

    @Then("the server responds with status 409")
    public void serverResponds409() {
        // 409 = CompanionNotUnlockedException at HTTP boundary.
        assertThat(companionError).isInstanceOf(CompanionNotUnlockedException.class);
        assertThat(creatureAudit.actionsFor(childId))
                .contains(CreatureAuditAction.COMPANION_PICK_REJECTED_NOT_UNLOCKED);
    }

    @And("the previous companion stays active")
    public void previousCompanionStaysActive() {
        assertThat(companions.findCompanion(childId)).contains(previousCompanion);
    }
}
