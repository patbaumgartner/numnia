package ch.numnia.dataexport.infra;

import ch.numnia.dataexport.domain.ExportFile;
import ch.numnia.dataexport.spi.ExportFileRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Volatile in-memory store; will move to Postgres + signed object storage. */
@Repository
public class InMemoryExportFileRepository implements ExportFileRepository {

    private final Map<UUID, ExportFile> byId = new ConcurrentHashMap<>();
    private final Map<String, UUID> tokenIndex = new ConcurrentHashMap<>();

    @Override
    public void save(ExportFile file) {
        byId.put(file.id(), file);
        tokenIndex.put(file.token(), file.id());
    }

    @Override
    public Optional<ExportFile> findByToken(String token) {
        UUID id = tokenIndex.get(token);
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<ExportFile> findById(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public List<ExportFile> findByParentId(UUID parentId) {
        List<ExportFile> result = new ArrayList<>();
        for (ExportFile f : byId.values()) {
            if (f.parentId().equals(parentId)) {
                result.add(f);
            }
        }
        return result;
    }

    @Override
    public List<ExportFile> findAll() {
        return new ArrayList<>(byId.values());
    }

    @Override
    public void delete(UUID id) {
        ExportFile removed = byId.remove(id);
        if (removed != null) {
            tokenIndex.remove(removed.token());
        }
    }
}
