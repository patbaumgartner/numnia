package ch.numnia.learning.infra;

import ch.numnia.learning.spi.StarPointsRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryStarPointsRepository implements StarPointsRepository {

    private final Map<UUID, Integer> balances = new ConcurrentHashMap<>();

    @Override
    public int balanceOf(UUID childId) {
        return balances.getOrDefault(childId, 0);
    }

    @Override
    public int addPoints(UUID childId, int delta) {
        return balances.merge(childId, delta, Integer::sum);
    }

    @Override
    public void setBalance(UUID childId, int balance) {
        balances.put(childId, balance);
    }
}
