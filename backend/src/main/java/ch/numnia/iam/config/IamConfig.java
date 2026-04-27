package ch.numnia.iam.config;

import ch.numnia.iam.spi.EmailGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

/**
 * IAM module configuration: password encoder, fantasy-name catalog, avatar catalog,
 * and email gateway.
 *
 * <p>Catalogs are currently inline constants (externalization to {@code application.yaml}
 * is deferred — follow-up item in .ralph/usecase-progress.md).
 *
 * <p>Fantasy names (BR-002): gender-neutral, safe, non-offensive, curated list.
 * Avatar base models (BR-003): gender-neutral identifiers.
 */
@Configuration
public class IamConfig {

    private static final Logger log = LoggerFactory.getLogger(IamConfig.class);

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

    /**
     * No-op email gateway for the current implementation (UC-002 lock notification).
     *
     * <p>The production implementation (SMTP relay) will replace this bean in a later
     * iteration. The no-op implementation logs a WARN so missing email sends are visible
     * in test output without any PII leaking (NFR-PRIV-001: no email address is logged).
     *
     * <p>TODO: replace with a real SMTP gateway in UC-009/notifications iteration.
     */
    @Bean
    public EmailGateway emailGateway() {
        return new EmailGateway() {
            @Override
            public void sendAccountLockedNotification(String parentEmail,
                                                     String childPseudonym,
                                                     String childOpaqueRef) {
                // Privacy: never log parentEmail (NFR-PRIV-001).
                log.warn("EmailGateway (no-op): lock notification for childRef={}", childOpaqueRef);
            }

            @Override
            public void sendDeletionConfirmationEmail(String parentEmail,
                                                     String childPseudonym,
                                                     String confirmationToken) {
                // Privacy: never log email or token (NFR-PRIV-001, UC-011).
                log.warn("EmailGateway (no-op): deletion confirmation requested for pseudonym={}",
                        childPseudonym);
            }

            @Override
            public void sendDeletionRecordEmail(String parentEmail,
                                                String childPseudonym,
                                                java.util.Set<String> dataCategories,
                                                java.time.Instant completedAt) {
                // Privacy: never log email (NFR-PRIV-001, UC-011 BR-002).
                log.warn("EmailGateway (no-op): deletion record for pseudonym={} categories={} at={}",
                        childPseudonym, dataCategories, completedAt);
            }
        };
    }
}

