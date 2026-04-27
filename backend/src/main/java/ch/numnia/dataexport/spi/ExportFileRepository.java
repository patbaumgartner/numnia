package ch.numnia.dataexport.spi;

import ch.numnia.dataexport.domain.ExportFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence boundary for {@link ExportFile} (UC-010). */
public interface ExportFileRepository {

    void save(ExportFile file);

    Optional<ExportFile> findByToken(String token);

    Optional<ExportFile> findById(UUID id);

    List<ExportFile> findByParentId(UUID parentId);

    /** All currently stored export files. Used by the purge job. */
    List<ExportFile> findAll();

    void delete(UUID id);

    /** Removes all export files belonging to the given child (UC-011). */
    int deleteByChildId(UUID childId);
}
