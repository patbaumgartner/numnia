package ch.numnia.iam;

import ch.numnia.iam.domain.*;
import ch.numnia.iam.service.*;
import ch.numnia.iam.spi.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ParentRegistrationService}.
 *
 * <p>Business rules covered:
 * <ul>
 *   <li>BR-001: Double opt-in (account created → token → verified)
 *   <li>BR-004: Data minimisation (consent flags mandatory)
 *   <li>BR-005: Audit trail entries for all relevant events
 * </ul>
 *
 * <p>Collaborators are mocked; this is a pure unit test (NFR-ENG-002).
 */
@ExtendWith(MockitoExtension.class)
class ParentRegistrationServiceTest {

    @Mock
    private ParentAccountRepository parents;
    @Mock
    private VerificationTokenRepository tokens;
    @Mock
    private AuditLogRepository auditLog;

    private PasswordEncoder passwordEncoder;
    private ParentRegistrationService service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(4); // low cost for tests
        service = new ParentRegistrationService(parents, tokens, auditLog, passwordEncoder);
    }

    // ─── BR-001 / happy path ───────────────────────────────────────────────

    @Nested
    @DisplayName("register() — happy path")
    class RegisterHappyPath {

        @Test
        @DisplayName("returns a UUID parent ID on successful registration")
        void register_withValidInput_returnsParentId() {
            when(parents.existsByEmail(anyString())).thenReturn(false);

            UUID parentId = service.register(
                    "parent@example.com", "securePass1", "Anna", "Frau", true, true);

            assertThat(parentId).isNotNull();
        }

        @Test
        @DisplayName("saves the parent account with status NOT_VERIFIED")
        void register_savesAccountWithNotVerifiedStatus() {
            when(parents.existsByEmail(anyString())).thenReturn(false);

            service.register("parent@example.com", "securePass1", "Anna", "Frau", true, true);

            ArgumentCaptor<ParentAccount> captor = ArgumentCaptor.forClass(ParentAccount.class);
            verify(parents).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(ParentStatus.NOT_VERIFIED);
        }

        @Test
        @DisplayName("hashes the password — plain text must not be stored (NFR-SEC-001, NFR-SEC-003)")
        void register_storesHashedPassword_notPlainText() {
            when(parents.existsByEmail(anyString())).thenReturn(false);

            service.register("parent@example.com", "securePass1", "Anna", "Frau", true, true);

            ArgumentCaptor<ParentAccount> captor = ArgumentCaptor.forClass(ParentAccount.class);
            verify(parents).save(captor.capture());
            String storedHash = captor.getValue().getHashedPassword();
            assertThat(storedHash).doesNotContain("securePass1");
            assertThat(passwordEncoder.matches("securePass1", storedHash)).isTrue();
        }

        @Test
        @DisplayName("creates a primary verification token (BR-001)")
        void register_createsEmailPrimaryVerificationToken() {
            when(parents.existsByEmail(anyString())).thenReturn(false);

            service.register("parent@example.com", "securePass1", "Anna", "Frau", true, true);

            ArgumentCaptor<VerificationToken> captor = ArgumentCaptor.forClass(VerificationToken.class);
            verify(tokens).save(captor.capture());
            assertThat(captor.getValue().getPurpose()).isEqualTo(TokenPurpose.EMAIL_PRIMARY);
            assertThat(captor.getValue().isConsumed()).isFalse();
        }

        @Test
        @DisplayName("writes ACCOUNT_CREATED audit log entry (BR-005)")
        void register_writesAuditLogEntry() {
            when(parents.existsByEmail(anyString())).thenReturn(false);

            service.register("parent@example.com", "securePass1", "Anna", "Frau", true, true);

            ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
            verify(auditLog).save(captor.capture());
            assertThat(captor.getValue().getAction()).isEqualTo(AuditAction.ACCOUNT_CREATED);
            // No email in audit log (NFR-PRIV-001)
            assertThat(captor.getValue().getParentRef()).doesNotContain("@");
        }
    }

    // ─── Duplicate email (flow 3b) ─────────────────────────────────────────

    @Nested
    @DisplayName("register() — duplicate email (flow 3b)")
    class RegisterDuplicateEmail {

        @Test
        @DisplayName("throws DuplicateEmailException when email is already registered")
        void register_withDuplicateEmail_throwsDuplicateEmailException() {
            when(parents.existsByEmail("dup@example.com")).thenReturn(true);

            assertThatThrownBy(() ->
                    service.register("dup@example.com", "securePass1", "Bob", "Herr", true, true))
                    .isInstanceOf(DuplicateEmailException.class);
        }

        @Test
        @DisplayName("logs DUPLICATE_REGISTRATION_BLOCKED for duplicate email (BR-005)")
        void register_withDuplicateEmail_logsAuditEntry() {
            when(parents.existsByEmail("dup@example.com")).thenReturn(true);

            assertThatThrownBy(() ->
                    service.register("dup@example.com", "pass1234", "Bob", "Herr", true, true))
                    .isInstanceOf(DuplicateEmailException.class);

            ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
            verify(auditLog).save(captor.capture());
            assertThat(captor.getValue().getAction())
                    .isEqualTo(AuditAction.DUPLICATE_REGISTRATION_BLOCKED);
        }

        @Test
        @DisplayName("does not create any account when email already exists")
        void register_withDuplicateEmail_doesNotSaveAccount() {
            when(parents.existsByEmail("dup@example.com")).thenReturn(true);

            assertThatThrownBy(() ->
                    service.register("dup@example.com", "pass1234", "Bob", "Herr", true, true))
                    .isInstanceOf(DuplicateEmailException.class);

            verify(parents, never()).save(any(ParentAccount.class));
        }
    }

    // ─── Validation failures (flow 3a) ────────────────────────────────────

    @Nested
    @DisplayName("register() — validation failures (flow 3a)")
    class RegisterValidation {

        @Test
        @DisplayName("throws when privacy consent is missing (BR-004)")
        void register_withMissingPrivacyConsent_isRejected() {
            assertThatThrownBy(() ->
                    service.register("p@example.com", "pass1234", "Anna", "Frau",
                            false /*privacyConsented*/, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Privacy consent");
        }

        @Test
        @DisplayName("throws when terms acceptance is missing (BR-004)")
        void register_withMissingTermsAcceptance_isRejected() {
            assertThatThrownBy(() ->
                    service.register("p@example.com", "pass1234", "Anna", "Frau",
                            true, false /*termsAccepted*/))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("throws when password is too short (< 8 chars)")
        void register_withWeakPassword_isRejected() {
            assertThatThrownBy(() ->
                    service.register("p@example.com", "short", "Anna", "Frau", true, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("8");
        }
    }

    // ─── verifyPrimaryEmail() ─────────────────────────────────────────────

    @Nested
    @DisplayName("verifyPrimaryEmail()")
    class VerifyPrimaryEmail {

        @Test
        @DisplayName("marks account as EMAIL_VERIFIED and consumes token (BR-001)")
        void verifyPrimaryEmail_happyPath_markAccountVerified() {
            UUID parentId = UUID.randomUUID();
            UUID tokenId = UUID.randomUUID();

            ParentAccount account = new ParentAccount(
                    parentId, "parent@example.com", "hashed", "Anna", "Frau", true, true);
            VerificationToken token = new VerificationToken(
                    parentId, null, TokenPurpose.EMAIL_PRIMARY);
            // Expose token UUID via reflection-like approach — use the public accessor
            when(tokens.findById(any())).thenReturn(Optional.of(token));
            when(parents.findById(parentId)).thenReturn(Optional.of(account));

            service.verifyPrimaryEmail(token.getId());

            assertThat(account.getStatus()).isEqualTo(ParentStatus.EMAIL_VERIFIED);
            assertThat(token.isConsumed()).isTrue();
        }

        @Test
        @DisplayName("throws TokenExpiredException for expired token (flow 5a)")
        void verifyPrimaryEmail_withExpiredToken_throwsTokenExpiredException() {
            UUID parentId = UUID.randomUUID();
            // Create token that expired 25 hours ago
            VerificationToken expiredToken = new VerificationToken(
                    parentId, null, TokenPurpose.EMAIL_PRIMARY,
                    java.time.Instant.now().minusSeconds(25 * 60 * 60));
            when(tokens.findById(any())).thenReturn(Optional.of(expiredToken));

            assertThatThrownBy(() -> service.verifyPrimaryEmail(expiredToken.getId()))
                    .isInstanceOf(TokenExpiredException.class);
        }

        @Test
        @DisplayName("throws TokenNotFoundException when token does not exist")
        void verifyPrimaryEmail_withUnknownToken_throwsTokenNotFoundException() {
            when(tokens.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.verifyPrimaryEmail(UUID.randomUUID()))
                    .isInstanceOf(TokenNotFoundException.class);
        }

        @Test
        @DisplayName("writes EMAIL_PRIMARY_VERIFIED audit entry (BR-005)")
        void verifyPrimaryEmail_writesAuditEntry() {
            UUID parentId = UUID.randomUUID();
            ParentAccount account = new ParentAccount(
                    parentId, "parent@example.com", "hashed", "Anna", "Frau", true, true);
            VerificationToken token = new VerificationToken(
                    parentId, null, TokenPurpose.EMAIL_PRIMARY);
            when(tokens.findById(any())).thenReturn(Optional.of(token));
            when(parents.findById(parentId)).thenReturn(Optional.of(account));

            service.verifyPrimaryEmail(token.getId());

            ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
            verify(auditLog).save(captor.capture());
            assertThat(captor.getValue().getAction()).isEqualTo(AuditAction.EMAIL_PRIMARY_VERIFIED);
        }
    }
}
