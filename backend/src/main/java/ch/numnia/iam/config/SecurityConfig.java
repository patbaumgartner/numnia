package ch.numnia.iam.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the IAM module.
 *
 * <p>UC-001 scope: all {@code /api/parents/**} registration endpoints are public
 * (no session exists before account creation). Full authn/authz is introduced
 * in UC-009 (parent area) and UC-002 (child sign-in).
 *
 * <p>CSRF is disabled for the REST API (stateless, no browser form sessions).
 *
 * <p>Rate limiting on verification endpoints (NFR-SEC-004) is deferred to
 * UC-009/UC-012 — documented as a follow-up in .ralph/usecase-progress.md.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Registration and verification flow — publicly accessible
                .requestMatchers("/api/parents/**").permitAll()
                // Child sign-in — publicly accessible (no session exists before sign-in)
                .requestMatchers("/api/child-sessions").permitAll()
                // Child sign-out — no Spring Security session check needed;
                // the SessionInterceptor handles revocation
                .requestMatchers("/api/child-sessions/**").permitAll()
                // E2E test helper — publicly accessible (gated by numnia.e2e.enabled in
                // TestTokenController; never registered in production profile)
                .requestMatchers("/api/test/**").permitAll()
                // Actuator health endpoint
                .requestMatchers("/actuator/health").permitAll()
                // All other requests require authentication (expanded in UC-009)
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
