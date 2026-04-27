package ch.numnia.creatures.infra;

import ch.numnia.creatures.domain.Creature;
import ch.numnia.creatures.spi.CreatureCatalog;
import ch.numnia.learning.domain.Operation;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Static catalogue of the three Release-1 creatures (UC-006).
 *
 * <p>Names are vetted in the use-case spec ("Pilzar", "Welleno", "Zacka")
 * and demonstrate BR-002: variable endings, no enforced "i" pattern.
 * Descriptions and any additional content will move to YAML with the
 * content catalogue (UC-007 follow-up).
 */
@Component
public class StaticCreatureCatalog implements CreatureCatalog {

    public static final String PILZAR_ID = "pilzar";
    public static final String WELLENO_ID = "welleno";
    public static final String ZACKA_ID = "zacka";

    private final List<Creature> creatures = List.of(
            new Creature(PILZAR_ID, "Pilzar", Operation.ADDITION, "mushroom-jungle"),
            new Creature(WELLENO_ID, "Welleno", Operation.MULTIPLICATION, "cloud-island"),
            new Creature(ZACKA_ID, "Zacka", Operation.SUBTRACTION, "crystal-cave"));

    @Override
    public List<Creature> listAll() {
        return creatures;
    }

    @Override
    public Optional<Creature> findById(String id) {
        return creatures.stream().filter(c -> c.id().equals(id)).findFirst();
    }
}
