package ch.numnia.parentcontrols.infra;

import ch.numnia.parentcontrols.domain.ChildControls;
import ch.numnia.parentcontrols.spi.ChildControlsRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryChildControlsRepository implements ChildControlsRepository {

    private final ConcurrentMap<UUID, ChildControls> store = new ConcurrentHashMap<>();

    @Override
    public Optional<ChildControls> findByChildId(UUID childId) {
        return Optional.ofNullable(store.get(childId));
    }

    @Override
    public void save(ChildControls controls) {
        store.put(controls.childId(), controls);
    }
}
