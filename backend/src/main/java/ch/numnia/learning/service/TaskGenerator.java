package ch.numnia.learning.service;

import ch.numnia.learning.domain.MathTask;
import ch.numnia.learning.domain.Operation;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.UUID;

/**
 * Generates a {@link MathTask} from the configured pool (FR-LEARN-003).
 *
 * <p>BR-001: results are guaranteed to be in [0, 1,000,000]. The generator
 * derives a per-(operation, S) operand range so the result cannot exceed the
 * ceiling, then samples uniformly using the supplied {@link Random}.
 */
@Component
public class TaskGenerator {

    public MathTask generate(Operation operation, int difficulty, int speed, Random rng) {
        return switch (operation) {
            case ADDITION -> additionTask(difficulty, speed, rng);
            case SUBTRACTION -> subtractionTask(difficulty, speed, rng);
            case MULTIPLICATION -> multiplicationTask(difficulty, speed, rng);
            case DIVISION -> divisionTask(difficulty, speed, rng);
        };
    }

    private MathTask additionTask(int s, int g, Random rng) {
        long max = operandMax(s);
        long a = randomLong(rng, max);
        long b = randomLong(rng, Math.max(0L, max - a));
        long sum = a + b;
        return new MathTask(UUID.randomUUID(), Operation.ADDITION,
                (int) a, (int) b, (int) sum, s, g);
    }

    private MathTask subtractionTask(int s, int g, Random rng) {
        long max = operandMax(s);
        long a = randomLong(rng, max);
        long b = randomLong(rng, a);
        return new MathTask(UUID.randomUUID(), Operation.SUBTRACTION,
                (int) a, (int) b, (int) (a - b), s, g);
    }

    private MathTask multiplicationTask(int s, int g, Random rng) {
        long maxA = pow10(Math.min(s, 6));
        long maxB = pow10(Math.min(Math.max(s - 1, 1), 6));
        long product;
        long a;
        long b;
        int safety = 100;
        do {
            a = 1 + randomLong(rng, Math.max(0L, maxA - 1));
            b = 1 + randomLong(rng, Math.max(0L, maxB - 1));
            product = a * b;
        } while (product > MathTask.MAX_RESULT && --safety > 0);
        if (product > MathTask.MAX_RESULT) {
            a = 2; b = 2; product = 4;
        }
        return new MathTask(UUID.randomUUID(), Operation.MULTIPLICATION,
                (int) a, (int) b, (int) product, s, g);
    }

    private MathTask divisionTask(int s, int g, Random rng) {
        long maxQuotient = pow10(Math.min(s, 6));
        long maxDivisor = pow10(Math.min(Math.max(s - 1, 1), 6));
        long divisor = 1 + randomLong(rng, Math.max(0L, maxDivisor - 1));
        long quotient = randomLong(rng, maxQuotient);
        long dividend = divisor * quotient;
        if (dividend > MathTask.MAX_RESULT) {
            quotient = MathTask.MAX_RESULT / divisor;
            dividend = divisor * quotient;
        }
        return new MathTask(UUID.randomUUID(), Operation.DIVISION,
                (int) dividend, (int) divisor, (int) quotient, s, g);
    }

    private static long operandMax(int s) {
        return Math.min(pow10(Math.max(s, 1)), MathTask.MAX_RESULT);
    }

    private static long pow10(int n) {
        long v = 1;
        for (int i = 0; i < Math.min(n, 6); i++) {
            v *= 10;
        }
        return v;
    }

    private static long randomLong(Random rng, long boundInclusive) {
        if (boundInclusive <= 0) {
            return 0;
        }
        return (long) (rng.nextDouble() * (boundInclusive + 1));
    }
}
