package ch.numnia.parentcontrols.infra;

import ch.numnia.parentcontrols.spi.PlayTimeLedger;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryPlayTimeLedger implements PlayTimeLedger {

    private record Key(UUID childId, LocalDate day) {}

    private final ConcurrentMap<Key, Integer> minutes = new ConcurrentHashMap<>();

    @Override
    public int minutesPlayedToday(UUID childId, LocalDate today) {
        return minutes.getOrDefault(new Key(childId, today), 0);
    }

    @Override
    public void addMinutes(UUID childId, LocalDate today, int delta) {
        if (delta <= 0) return;
        minutes.merge(new Key(childId, today), delta, Integer::sum);
    }
}
