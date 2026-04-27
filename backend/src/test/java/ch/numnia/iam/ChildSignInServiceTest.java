package ch.numnia.iam;

import ch.numnia.iam.domain.*;
import ch.numnia.iam.service.*;
import ch.numnia.iam.spi.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ChildSignInService} (UC-002).
 *
 * <p>Business rules covered:
 * <ul>
 *   <li>BR-001: children cannot sign in with parent credentials
 *   <li>BR-002: returned session has role CHILD (least privilege)
 *   <li>BR-003: PIN validation — 4-6 digits only
 *   <li>BR-004: lockout after 5 failed attempts; parent-only lock release
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ChildSignInServiceTest {

    @Mock private ChildProfileRepository childProfiles;
    @Mock private ParentAccountRepository parents;
    @Mock private ChildSessionRepository sessions;
    @Mock private AuditLogRepository auditLog;
    @Mock private EmailGateway emailGateway;

    /**
     * Real BCryptPasswordEncoder so PIN matching tests work correctly.
     * BCrypt 4 rounds is fast enough for tests.
     */
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private ChildSignInService service;

    @BeforeEach
    void setUp() {
        service = new ChildSignInService(
                childProfiles, parents, sessions, auditLog, passwordEncoder, emailGateway);
    }

    // ── test helpers ──────────────────────────────────────────────────────

    private ChildProfile activeProfileWithPin(UUID childId, UUID parentId, String rawPin) {
        ChildProfile profile = new ChildProfile(
                childId, "Luna", LocalDate.now().getYear() - 9, "star", parentId);
        profile.activate();
        profile.setPinHash(passwordEncoder.encode(rawPin));
        return profile;
    }

    private ChildProfile activeProfileWithoutPin(UUID childId, UUID parentId) {
        ChildProfile profile = new ChildProfile(
                childId, "Luna", LocalDate.now().getYear() - 9, "star", parentId);
        profile.activate();
        return profile;
    }

    private ParentAccount parentAccount(UUID parentId) {
        ParentAccount account = new ParentAccount(
                parentId, "parent@example.com", "hashed", "Anna", "Frau", true, true);
        account.markEmailVerified();
        account.markFullyConsented();
        return account;
    }

    // ── setPin() ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("setPin() — PIN management (BR-003)")
    class SetPin {

        @Test
        @DisplayName("stores BCrypt hash of a valid 4-digit PIN")
        void setPin_withFourDigitPin_storesBcryptHash() {
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            ChildProfile profile = activeProfileWithoutPin(childId, parentId);
            when(childProfiles.findById(childId)).thenReturn(Optional.of(profile));

            service.setPin(parentId, childId, "1234");

            assertThat(profile.hasPinSet()).isTrue();
            assertThat(profile.getPinHash()).isNotEqualTo("1234"); // never stored as plaintext
            assertThat(passwordEncoder.matches("1234", profile.getPinHash())).isTrue();
        }

        @Test
        @DisplayName("stores BCrypt hash of a valid 6-digit PIN")
        void setPin_withSixDigitPin_isAccepted() {
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            ChildProfile profile = activeProfileWithoutPin(childId, parentId);
            when(childProfiles.findById(childId)).thenReturn(Optional.of(profile));

            service.setPin(parentId, childId, "123456");

            assertThat(profile.hasPinSet()).isTrue();
        }

        @Test
        @DisplayName("rejects PIN with fewer than 4 digits (BR-003)")
        void setPin_withFewerThanFourDigits_isRejected() {
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();

            assertThatThrownBy(() -> service.setPin(parentId, childId, "123"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("4 to 6 digits");
        }

        @Test
        @DisplayName("rejects PIN with more than 6 digits (BR-003)")
        void setPin_withMoreThanSixDigits_isRejected() {
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();

            assertThatThrownBy(() -> service.setPin(parentId, childId, "1234567"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects PIN with non-digit characters (BR-003)")
        void setPin_withNonDigits_isRejected() {
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();

            assertThatThrownBy(() -> service.setPin(parentId, childId, "12ab"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects null PIN")
        void setPin_withNullPin_isRejected() {
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();

            assertThatThrownBy(() -> service.setPin(parentId, childId, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("throws UnauthorizedParentException for wrong parent (server-side authz)")
        void setPin_withWrongParent_isRejected() {
            UUID ownerParentId = UUID.randomUUID();
            UUID otherParentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            ChildProfile profile = activeProfileWithoutPin(childId, ownerParentId);
            when(childProfiles.findById(childId)).thenReturn(Optional.of(profile));

            assertThatThrownBy(() -> service.setPin(otherParentId, childId, "1234"))
                    .isInstanceOf(UnauthorizedParentException.class);
        }

        @Test
        @DisplayName("writes CHILD_PIN_SET audit entry (NFR-SEC-001)")
        void setPin_writesAuditEntry() {
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            ChildProfile profile = activeProfileWithoutPin(childId, parentId);
            when(childProfiles.findById(childId)).thenReturn(Optional.of(profile));

            service.setPin(parentId, childId, "1234");

            ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
            verify(auditLog).save(captor.capture());
            assertThat(captor.getValue().getAction()).isEqualTo(AuditAction.CHILD_PIN_SET);
            assertThat(captor.getValue().getParentRef()).doesNotContain("@"); // no PII
        }
    }

    // ── signIn() ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("signIn() — happy path (UC-002 main flow)")
    class SignInHappyPath {

        @Test
        @DisplayName("returns a CHILD-role session on correct PIN (BR-002)")
        void signIn_withCorrectPin_returnsChildSession() {
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            ChildProfile profile = activeProfileWithPin(childId, parentId, "1234");
            when(childProfiles.findById(childId)).thenReturn(Optional.of(profile));

            ChildSession session = service.signIn(childId, "1234");

            assertThat(session).isNotNull();
            assertThat(session.getRole()).isEqualTo("CHILD");
            assertThat(session.getChildId()).isEqualTo(childId);
            assertThat(session.isValid()).isTrue();
        }

        @Test
        @DisplayName("session has least-privilege CHILD role — not PARENT (BR-002)")
        void session_byDefaultIsChildRoleWithRestrictedRights_brLeastPrivilege() {
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            ChildProfile profile = activeProfileWithPin(childId, parentId, "5678");
            when(childProfiles.findById(childId)).thenReturn(Optional.of(profile));

            ChildSession session = service.signIn(childId, "5678");

            assertThat(session.getRole()).isEqualTo("CHILD");
            assertThat(session.getRole()).doesNotContain("PARENT");
        }

        @Test
        @DisplayName("saves session to repository on successful sign-in")
        void signIn_persistsSession() {
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            ChildProfile profile = activeProfileWithPin(childId, parentId, "1234");
            when(childProfiles.findById(childId)).thenReturn(Optional.of(profile));

            service.signIn(childId, "1234");

            verify(sessions).save(any(ChildSession.class));
        }

        @Test
        @DisplayName("writes CHILD_SIGNED_IN audit entry")
        void signIn_writesAuditEntry() {
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            ChildProfile profile = activeProfileWithPin(childId, parentId, "1234");
            when(childProfiles.findById(childId)).thenReturn(Optional.of(profile));

            service.signIn(childId, "1234");

            ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
            verify(auditLog, atLeastOnce()).save(captor.capture());
            assertThat(captor.getAllValues())
                    .extracting(AuditLogEntry::getAction)
                    .contains(AuditAction.CHILD_SIGNED_IN);
        }

        @Test
        @DisplayName("resets failed attempt counter on successful sign-in")
        void signIn_resetsFailedCounter() {
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            ChildProfile profile = activeProfileWithPin(childId, parentId, "1234");
            // Simulate 2 prior failures (without locking)
            profile.recordFailedSignIn();
            profile.recordFailedSignIn();
            when(childProfiles.findById(childId)).thenReturn(Optional.of(profile));

            service.signIn(childId, "1234");

            assertThat(profile.getFailedSignInCount()).isZero();
        }
    }

    // ── signIn() failure paths ────────────────────────────────────────────

    @Nested
    @DisplayName("signIn() — failure paths (UC-002 alt flow 5a, BR-004)")
    class SignInFailurePaths {

        @Test
        @DisplayName("throws InvalidPinException for wrong PIN (alt flow 5a)")
        void signIn_withWrongPin_throwsInvalidPinException() {
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            ChildProfile profile = activeProfileWithPin(childId, parentId, "1234");
            when(childProfiles.findById(childId)).thenReturn(Optional.of(profile));

            assertThatThrownBy(() -> service.signIn(childId, "9999"))
                    .isInstanceOf(InvalidPinException.class);
        }

        @Test
        @DisplayName("increments failed attempt counter on wrong PIN")
        void signIn_incrementsFailedCounter_onWrongPin() {
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            ChildProfile profile = activeProfileWithPin(childId, parentId, "1234");
            when(childProfiles.findById(childId)).thenReturn(Optional.of(profile));

            assertThatThrownBy(() -> service.signIn(childId, "0000"))
                    .isInstanceOf(InvalidPinException.class);

            assertThat(profile.getFailedSignInCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("locks profile after 5 consecutive wrong PINs (BR-004)")
        void signIn_withWrongPinFiveTimes_locksProfile() {
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            ChildProfile profile = activeProfileWithPin(childId, parentId, "1234");
            when(childProfiles.findById(childId)).thenReturn(Optional.of(profile));

            for (int i = 0; i < 4; i++) {
                assertThatThrownBy(() -> service.signIn(childId, "9999"))
                        .isInstanceOf(InvalidPinException.class);
            }
            // 5th attempt triggers lockout
            assertThatThrownBy(() -> service.signIn(childId, "9999"))
                    .isInstanceOf(ProfileLockedException.class);

            assertThat(profile.isLocked()).isTrue();
        }

        @Test
        @DisplayName("sends lock notification email to parent after 5 failures (BR-004)")
        void signIn_withWrongPinFiveTimes_notifiesParent() {
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            ChildProfile profile = activeProfileWithPin(childId, parentId, "1234");
            ParentAccount parent = parentAccount(parentId);
            when(childProfiles.findById(childId)).thenReturn(Optional.of(profile));
            when(parents.findById(parentId)).thenReturn(Optional.of(parent));

            for (int i = 0; i < 5; i++) {
                assertThatThrownBy(() -> service.signIn(childId, "9999"))
                        .isInstanceOf(RuntimeException.class);
            }

            verify(emailGateway).sendAccountLockedNotification(
                    eq("parent@example.com"),
                    eq("Luna"),
                    eq(childId.toString()));
        }

        @Test
        @DisplayName("writes CHILD_PROFILE_LOCKED audit entry on 5th failure (BR-004)")
        void signIn_withWrongPinFiveTimes_locksProfileAndAuditsLock() {
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            ChildProfile profile = activeProfileWithPin(childId, parentId, "1234");
            when(childProfiles.findById(childId)).thenReturn(Optional.of(profile));
            when(parents.findById(parentId)).thenReturn(Optional.of(parentAccount(parentId)));

            for (int i = 0; i < 5; i++) {
                assertThatThrownBy(() -> service.signIn(childId, "9999"))
                        .isInstanceOf(RuntimeException.class);
            }

            ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
            verify(auditLog, atLeast(5)).save(captor.capture());
            assertThat(captor.getAllValues())
                    .extracting(AuditLogEntry::getAction)
                    .contains(AuditAction.CHILD_PROFILE_LOCKED);
        }

        @Test
        @DisplayName("throws ProfileLockedException immediately for subsequent attempts on locked profile")
        void signIn_onLockedProfile_rejectsImmediately() {
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            ChildProfile profile = activeProfileWithPin(childId, parentId, "1234");
            // Pre-lock the profile
            for (int i = 0; i < 5; i++) { profile.recordFailedSignIn(); }
            when(childProfiles.findById(childId)).thenReturn(Optional.of(profile));

            assertThatThrownBy(() -> service.signIn(childId, "1234")) // even correct PIN
                    .isInstanceOf(ProfileLockedException.class);
        }

        @Test
        @DisplayName("throws ChildNotFoundException for unknown childId")
        void signIn_withUnknownChildId_throwsChildNotFoundException() {
            UUID childId = UUID.randomUUID();
            when(childProfiles.findById(childId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.signIn(childId, "1234"))
                    .isInstanceOf(ChildNotFoundException.class);
        }

        @Test
        @DisplayName("throws IllegalStateException when no PIN has been set yet")
        void signIn_withNoPinSet_throwsIllegalState() {
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            ChildProfile profile = activeProfileWithoutPin(childId, parentId);
            when(childProfiles.findById(childId)).thenReturn(Optional.of(profile));

            assertThatThrownBy(() -> service.signIn(childId, "1234"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ── releaseLock() ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("releaseLock() — parent releases a locked profile (BR-004)")
    class ReleaseLock {

        @Test
        @DisplayName("releases a locked profile — profile is no longer locked")
        void releaseLock_unlocksProfile() {
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            ChildProfile profile = activeProfileWithPin(childId, parentId, "1234");
            for (int i = 0; i < 5; i++) { profile.recordFailedSignIn(); }
            assertThat(profile.isLocked()).isTrue();
            when(childProfiles.findById(childId)).thenReturn(Optional.of(profile));

            service.releaseLock(parentId, childId);

            assertThat(profile.isLocked()).isFalse();
            assertThat(profile.getFailedSignInCount()).isZero();
        }

        @Test
        @DisplayName("writes CHILD_LOCK_RELEASED audit entry")
        void releaseLock_writesAuditEntry() {
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            ChildProfile profile = activeProfileWithPin(childId, parentId, "1234");
            for (int i = 0; i < 5; i++) { profile.recordFailedSignIn(); }
            when(childProfiles.findById(childId)).thenReturn(Optional.of(profile));

            service.releaseLock(parentId, childId);

            ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
            verify(auditLog).save(captor.capture());
            assertThat(captor.getValue().getAction()).isEqualTo(AuditAction.CHILD_LOCK_RELEASED);
        }

        @Test
        @DisplayName("rejects release by a different parent (server-side authz)")
        void releaseLock_byWrongParent_isRejected() {
            UUID ownerParentId = UUID.randomUUID();
            UUID otherParentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            ChildProfile profile = activeProfileWithPin(childId, ownerParentId, "1234");
            for (int i = 0; i < 5; i++) { profile.recordFailedSignIn(); }
            when(childProfiles.findById(childId)).thenReturn(Optional.of(profile));

            assertThatThrownBy(() -> service.releaseLock(otherParentId, childId))
                    .isInstanceOf(UnauthorizedParentException.class);
        }

        @Test
        @DisplayName("throws IllegalStateException if profile is not locked")
        void releaseLock_onUnlockedProfile_throwsIllegalState() {
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            ChildProfile profile = activeProfileWithPin(childId, parentId, "1234");
            when(childProfiles.findById(childId)).thenReturn(Optional.of(profile));

            assertThatThrownBy(() -> service.releaseLock(parentId, childId))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ── signOut() ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("signOut() — session revocation (UC-002 main flow)")
    class SignOut {

        @Test
        @DisplayName("revokes an active session — session is no longer valid")
        void signOut_revokesActiveSession() {
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            ChildSession session = new ChildSession(childId, parentId);
            when(sessions.findById(session.getId())).thenReturn(Optional.of(session));

            service.signOut(session.getId());

            assertThat(session.isValid()).isFalse();
            assertThat(session.getRevokedAt()).isNotNull();
            verify(sessions).save(session);
        }

        @Test
        @DisplayName("no-op if session token does not exist (idempotent)")
        void signOut_withUnknownToken_isNoOp() {
            UUID unknownToken = UUID.randomUUID();
            when(sessions.findById(unknownToken)).thenReturn(Optional.empty());

            assertThatNoException().isThrownBy(() -> service.signOut(unknownToken));
        }
    }

    // ── BR-001: parent credentials must not work for child sign-in ─────────

    @Nested
    @DisplayName("BR-001: children cannot sign in with parent credentials")
    class Br001ParentCredentialsSeparation {

        @Test
        @DisplayName("sign-in API takes childId + PIN only — no parent email accepted")
        void signIn_apiOnlyAcceptsChildIdAndPin_notParentEmail() {
            // The signIn() method signature enforces this: UUID childId + rawPin.
            // There is no overload that accepts a parent email or password.
            // Verify: calling signIn with a UUID that points to no child profile
            // throws ChildNotFoundException, not a different exception path.
            UUID nonExistentChildId = UUID.randomUUID();
            when(childProfiles.findById(nonExistentChildId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.signIn(nonExistentChildId, "1234"))
                    .isInstanceOf(ChildNotFoundException.class);
        }
    }
}
