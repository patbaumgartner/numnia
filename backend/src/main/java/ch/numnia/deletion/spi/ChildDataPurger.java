package ch.numnia.deletion.spi;

import java.util.Set;
import java.util.UUID;

/**
 * Strategy that removes one module's child-scoped data on deletion (UC-011).
 *
 * <p>Each module contributes one implementation. The returned set contains the
 * data-category labels that were touched (used to populate the deletion record
 * email and audit details, BR-002). Implementations MUST be idempotent: a
 * second invocation for an already-purged child returns an empty set.
 */
public interface ChildDataPurger {

    /**
     * Purge all data this purger owns for the given child.
     *
     * @param childId child profile UUID
     * @return data-category labels that were actually purged (may be empty)
     */
    Set<String> purge(UUID childId);
}
