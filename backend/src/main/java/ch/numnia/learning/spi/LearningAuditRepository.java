package ch.numnia.learning.spi;

import ch.numnia.learning.domain.LearningAuditAction;
import ch.numnia.learning.domain.LearningAuditEntry;

import java.util.List;
import java.util.UUID;

public interface LearningAuditRepository {
    LearningAuditEntry append(UUID childRef, LearningAuditAction action, String detail);
    List<LearningAuditEntry> findByChildRef(UUID childRef);
}
