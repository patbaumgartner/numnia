package ch.numnia.learning;

import ch.numnia.learning.domain.MathTask;
import ch.numnia.learning.domain.Operation;
import ch.numnia.learning.service.TaskGenerator;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for {@link TaskGenerator} — BR-001 (results ≤ 1,000,000). */
class TaskGeneratorTest {

    private final TaskGenerator generator = new TaskGenerator();

    @RepeatedTest(50)
    void generate_addition_S6_resultIsWithinRangeUpTo1Million() {
        Random rng = new Random(42);
        MathTask task = generator.generate(Operation.ADDITION, 6, 2, rng);

        assertThat(task.expectedAnswer()).isBetween(0, 1_000_000);
        assertThat(task.operandA() + task.operandB()).isEqualTo(task.expectedAnswer());
    }

    @RepeatedTest(50)
    void generate_multiplication_resultIsWithinRangeUpTo1Million() {
        Random rng = new Random(7);
        MathTask task = generator.generate(Operation.MULTIPLICATION, 5, 2, rng);

        assertThat(task.expectedAnswer()).isBetween(0, 1_000_000);
        assertThat((long) task.operandA() * task.operandB()).isEqualTo(task.expectedAnswer());
    }

    @Test
    void generate_subtraction_doesNotProduceNegativeResults() {
        Random rng = new Random(11);
        for (int i = 0; i < 100; i++) {
            MathTask task = generator.generate(Operation.SUBTRACTION, 4, 2, rng);
            assertThat(task.expectedAnswer()).isGreaterThanOrEqualTo(0);
            assertThat(task.operandA() - task.operandB()).isEqualTo(task.expectedAnswer());
        }
    }

    @Test
    void generate_division_resultsExactly() {
        Random rng = new Random(101);
        for (int i = 0; i < 100; i++) {
            MathTask task = generator.generate(Operation.DIVISION, 4, 2, rng);
            assertThat(task.operandB()).isPositive();
            assertThat(task.operandA() % task.operandB()).isZero();
            assertThat(task.operandA() / task.operandB()).isEqualTo(task.expectedAnswer());
        }
    }
}
