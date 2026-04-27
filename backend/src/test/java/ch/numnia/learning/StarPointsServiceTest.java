package ch.numnia.learning;

import ch.numnia.learning.domain.AnswerOutcome;
import ch.numnia.learning.infra.InMemoryStarPointsRepository;
import ch.numnia.learning.service.StarPointsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StarPointsServiceTest {

    private InMemoryStarPointsRepository repo;
    private StarPointsService service;
    private final UUID childId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repo = new InMemoryStarPointsRepository();
        service = new StarPointsService(repo);
    }

    @Test
    void reward_wrongAnswer_keepsBalance_brNoPenalty() {
        repo.setBalance(childId, 12);

        int balance = service.reward(childId, AnswerOutcome.WRONG);

        assertThat(balance).isEqualTo(12);
        assertThat(repo.balanceOf(childId)).isEqualTo(12);
    }

    @Test
    void reward_timeoutAnswer_keepsBalance_brNoPenalty() {
        repo.setBalance(childId, 5);

        int balance = service.reward(childId, AnswerOutcome.TIMEOUT);

        assertThat(balance).isEqualTo(5);
    }

    @Test
    void reward_correctAnswer_awardsPoints() {
        repo.setBalance(childId, 10);

        int balance = service.reward(childId, AnswerOutcome.CORRECT);

        assertThat(balance).isEqualTo(11);
    }
}
