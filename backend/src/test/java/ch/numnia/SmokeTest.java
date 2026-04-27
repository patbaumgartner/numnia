package ch.numnia;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bootstrap smoke test — verifies the test infrastructure wires up.
 *
 * <p>Intentionally trivial: replaced by meaningful UC tests as each use
 * case is implemented test-first (NFR-ENG-002).
 */
class SmokeTest {

    @Test
    void smoke() {
        assertTrue(true, "Test infrastructure is operational");
    }
}
