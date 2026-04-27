package ch.numnia;

import io.cucumber.junit.platform.engine.Constants;
import org.junit.platform.suite.api.*;

/**
 * JUnit Platform Suite runner for Cucumber BDD scenarios.
 *
 * <p>Discovers all feature files under {@code classpath:features/} and uses
 * the glue path {@code ch.numnia.iam.steps} for step definitions.
 *
 * <p>Spring context is initialised by {@link CucumberSpringConfiguration}
 * via the {@code cucumber-spring} integration.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(
        key = Constants.GLUE_PROPERTY_NAME,
        value = "ch.numnia.iam.steps,ch.numnia")
@ConfigurationParameter(
        key = Constants.PLUGIN_PROPERTY_NAME,
        value = "pretty,html:target/cucumber-reports/cucumber.html")
@ConfigurationParameter(
        key = Constants.FILTER_TAGS_PROPERTY_NAME,
        value = "not @wip")
public class CucumberRunnerTest {
    // Suite runner — no methods required.
}
