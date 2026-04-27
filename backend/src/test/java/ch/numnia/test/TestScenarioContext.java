package ch.numnia.test;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Shared per-scenario state used to bridge step definitions across
 * use-case slices (e.g. Uc002 sets the active childId, Uc003/Uc007 read it).
 */
@Component
public class TestScenarioContext {

    private UUID childId;
    private UUID parentId;

    public void reset() {
        childId = null;
        parentId = null;
    }

    public void setChildId(UUID childId) {
        this.childId = childId;
    }

    public UUID childId() {
        return childId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }

    public UUID parentId() {
        return parentId;
    }
}
