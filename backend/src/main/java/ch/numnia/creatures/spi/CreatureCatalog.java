package ch.numnia.creatures.spi;

import ch.numnia.creatures.domain.Creature;

import java.util.List;
import java.util.Optional;

/** Catalogue of available creatures (FR-CRE-001). */
public interface CreatureCatalog {

    List<Creature> listAll();

    Optional<Creature> findById(String id);
}
