package ch.numnia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Numnia application entry point.
 *
 * <p>This class bootstraps the Spring Boot application. Business modules are
 * organised as Spring Modulith modules under sub-packages (e.g.
 * {@code ch.numnia.registration}, {@code ch.numnia.session}).
 */
@SpringBootApplication
public class NumniaApplication {

    public static void main(String[] args) {
        SpringApplication.run(NumniaApplication.class, args);
    }
}
