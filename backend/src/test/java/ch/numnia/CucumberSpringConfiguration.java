package ch.numnia;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Binds the Spring Boot test application context to Cucumber scenarios.
 *
 * <p>The {@code test} profile activates {@code application-test.yaml}, which
 * configures an H2 in-memory database (Flyway disabled, ddl-auto=create-drop)
 * and enables the e2e test-helper endpoint ({@code numnia.e2e.enabled=true}).
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CucumberSpringConfiguration {
    // Configuration only — no members required.
}
