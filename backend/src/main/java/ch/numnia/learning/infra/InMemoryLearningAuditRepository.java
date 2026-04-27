package ch.numnia.learning.infra;

import ch.numnia.learning.domain.LearningAuditAction;
import ch.numnia.learning.domain.LearningAuditEntry;
import ch.numnia.learning.spi.LearningAuditRepository;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class InMemoryLearningAuditRepository implements LearningAuditRepository {

    private final List<LearningAuditEntry> entries = new ArrayList<>();
    private final Clock clock;

    public InMemoryLearningAuditRepository() {
        this(Clock.systemUTC());
    }

    InMemoryLearningAuditRepository(Clock clock) {
        this.clock = clock;
    }

    @Override
    public synchronized LearningAuditEntry append(UUID childRef, LearningAuditAction action, String detail) {
        var entry = new LearningAuditEntry(
                UUID.randomUUID(), childRef, action, detail, clock.instant());
        entries.add(entry);
        return entry;
    }

    @Override
    public synchronized List<LearningAuditEntry> findByChildRef(UUID childRef) {
        return entries.stream()
                .filter(e -> childRef.equals(e.childRef()))
                .collect(Collectors.toUnmodifiableList());
    }
}
