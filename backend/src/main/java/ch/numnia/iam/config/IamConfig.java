package ch.numnia.iam.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;

/**
 * IAM module configuration: password encoder, fantasy-name catalog, avatar catalog.
 *
 * <p>Catalogs are currently inline constants (externalization to {@code application.yaml}
 * is deferred — follow-up item in .ralph/usecase-progress.md).
 *
 * <p>Fantasy names (BR-002): gender-neutral, safe, non-offensive, curated list.
 * Avatar base models (BR-003): gender-neutral identifiers.
 */
@Configuration
public class IamConfig {

    /**
     * BCrypt password encoder (NFR-SEC-001, NFR-SEC-003).
     * Strength 10 is the recommended default.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * Vetted fantasy-name catalog (BR-002, FR-SAFE-003).
     *
     * <p>TODO: externalize to {@code application.yaml} under
     * {@code numnia.iam.fantasy-names} — deferred follow-up.
     */
    @Bean
    public Set<String> fantasyNameCatalog() {
        return Set.of(
                "Astra", "Blitz", "Comet", "Deva", "Echo",
                "Flair", "Glint", "Halo", "Iris", "Jade",
                "Kite", "Luna", "Miro", "Nova", "Orion",
                "Pixel", "Quest", "Rho", "Sol", "Terra",
                "Uno", "Vega", "Wave", "Xeno", "Yuki", "Zara"
        );
    }

    /**
     * Gender-neutral avatar base-model catalog (BR-003, FR-CRE-005).
     *
     * <p>TODO: externalize to {@code application.yaml} — deferred follow-up.
     */
    @Bean
    public Set<String> avatarBaseModelCatalog() {
        return Set.of("star", "moon", "sun", "cloud", "wave", "rock", "fire", "wind");
    }
}
