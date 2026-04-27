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

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ChildProfileService}.
 *
 * <p>Business rules covered:
 * <ul>
 *   <li>BR-002: Fantasy name must be from vetted catalog
 *   <li>BR-003: Avatar must be from gender-neutral catalog
 *   <li>BR-004: Data minimisation — child carries pseudonym only
 *   <li>BR-005: Audit trail entries
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ChildProfileServiceTest {

    @Mock
    private ParentAccountRepository parents;
    @Mock
    private ChildProfileRepository childProfiles;
    @Mock
    private VerificationTokenRepository tokens;
    @Mock
    private AuditLogRepository auditLog;

    private static final Set<String> FANTASY_NAMES = Set.of("Luna", "Nova", "Orion");
    private static final Set<String> AVATARS = Set.of("star", "moon", "sun");

    private ChildProfileService service;

    @BeforeEach
    void setUp() {
        service = new ChildProfileService(
                parents, childProfiles, tokens, auditLog, FANTASY_NAMES, AVATARS);
    }

    private ParentAccount verifiedParent() {
        UUID id = UUID.randomUUID();
        ParentAccount account = new ParentAccount(
                id, "p@example.com", "hashed", "Anna", "Frau", true, true);
        account.markEmailVerified();
        return account;
    }

    private int validYearOfBirth() {
        return LocalDate.now().getYear() - 9; // age 9
    }

    // ─── Happy path ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("createChildProfile() — happy path")
    class CreateHappyPath {

        @Test
        @DisplayName("returns a child profile UUID for valid inputs")
        void createChildProfile_withValidInput_returnsChildId() {
            ParentAccount parent = verifiedParent();
            when(parents.findById(parent.getId())).thenReturn(Optional.of(parent));

            UUID childId = service.createChildProfile(
                    parent.getId(), "Luna", validYearOfBirth(), "star");

            assertThat(childId).isNotNull();
        }

        @Test
        @DisplayName("saves child profile with pseudonym only — no real name (BR-004)")
        void childProfile_doesNotCarryRealName_dataMinimization() {
            ParentAccount parent = verifiedParent();
            when(parents.findById(parent.getId())).thenReturn(Optional.of(parent));

            service.createChildProfile(parent.getId(), "Luna", validYearOfBirth(), "star");

            ArgumentCaptor<ChildProfile> captor = ArgumentCaptor.forClass(ChildProfile.class);
            verify(childProfiles).save(captor.capture());
            ChildProfile saved = captor.getValue();
            // BR-004: child entity must not carry a real name
            assertThat(saved.getPseudonym()).isEqualTo("Luna");
            // There must be no 'realName' or 'name' field — pseudonym is the only identifier
            // pinHash, lockedAt, lockedReason are intentionally null until UC-002 sets them
            assertThat(saved).hasNoNullFieldsOrPropertiesExcept("pinHash", "lockedAt", "lockedReason");
        }

        @Test
        @DisplayName("creates child with status PENDING_CONFIRM")
        void createChildProfile_statusIsPendingConfirm() {
            ParentAccount parent = verifiedParent();
            when(parents.findById(parent.getId())).thenReturn(Optional.of(parent));

            service.createChildProfile(parent.getId(), "Nova", validYearOfBirth(), "moon");

            ArgumentCaptor<ChildProfile> captor = ArgumentCaptor.forClass(ChildProfile.class);
            verify(childProfiles).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(ChildStatus.PENDING_CONFIRM);
        }

        @Test
        @DisplayName("multiplayer is disabled on creation (R1 rule, main flow step 11)")
        void createChildProfile_multiplayerIsDisabled() {
            ParentAccount parent = verifiedParent();
            when(parents.findById(parent.getId())).thenReturn(Optional.of(parent));

            service.createChildProfile(parent.getId(), "Orion", validYearOfBirth(), "sun");

            ArgumentCaptor<ChildProfile> captor = ArgumentCaptor.forClass(ChildProfile.class);
            verify(childProfiles).save(captor.capture());
            assertThat(captor.getValue().isMultiplayerEnabled()).isFalse();
        }

        @Test
        @DisplayName("creates EMAIL_SECONDARY verification token (BR-001)")
        void createChildProfile_createsSecondaryToken() {
            ParentAccount parent = verifiedParent();
            when(parents.findById(parent.getId())).thenReturn(Optional.of(parent));

            service.createChildProfile(parent.getId(), "Luna", validYearOfBirth(), "star");

            ArgumentCaptor<VerificationToken> tokenCaptor =
                    ArgumentCaptor.forClass(VerificationToken.class);
            verify(tokens).save(tokenCaptor.capture());
            assertThat(tokenCaptor.getValue().getPurpose()).isEqualTo(TokenPurpose.EMAIL_SECONDARY);
        }

        @Test
        @DisplayName("writes CHILD_PROFILE_CREATED audit log entry (BR-005)")
        void createChildProfile_writesAuditLogEntry() {
            ParentAccount parent = verifiedParent();
            when(parents.findById(parent.getId())).thenReturn(Optional.of(parent));

            service.createChildProfile(parent.getId(), "Luna", validYearOfBirth(), "star");

            ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
            verify(auditLog).save(captor.capture());
            assertThat(captor.getValue().getAction()).isEqualTo(AuditAction.CHILD_PROFILE_CREATED);
            assertThat(captor.getValue().getChildRef()).isEqualTo("Luna");
            // No email in audit log
            assertThat(captor.getValue().getParentRef()).doesNotContain("@");
        }

        @Test
        @DisplayName("accepts year of birth at age boundary 12 (upper bound)")
        void createChildProfile_atAgeBoundaryTwelve_isAccepted() {
            ParentAccount parent = verifiedParent();
            when(parents.findById(parent.getId())).thenReturn(Optional.of(parent));
            int yearForAge12 = LocalDate.now().getYear() - 12;

            UUID childId = service.createChildProfile(
                    parent.getId(), "Luna", yearForAge12, "star");
            assertThat(childId).isNotNull();
        }

        @Test
        @DisplayName("accepts year of birth at age boundary 7 (lower bound)")
        void createChildProfile_atAgeBoundarySeven_isAccepted() {
            ParentAccount parent = verifiedParent();
            when(parents.findById(parent.getId())).thenReturn(Optional.of(parent));
            int yearForAge7 = LocalDate.now().getYear() - 7;

            UUID childId = service.createChildProfile(
                    parent.getId(), "Luna", yearForAge7, "star");
            assertThat(childId).isNotNull();
        }
    }

    // ─── BR-002: Fantasy name validation ──────────────────────────────────

    @Nested
    @DisplayName("createChildProfile() — fantasy name validation (BR-002)")
    class FantasyNameValidation {

        @Test
        @DisplayName("rejects fantasy name not in vetted catalog (BR-002)")
        void createChildProfile_withFantasyNameOutsideCatalog_isRejected() {
            ParentAccount parent = verifiedParent();
            when(parents.findById(parent.getId())).thenReturn(Optional.of(parent));

            assertThatThrownBy(() ->
                    service.createChildProfile(parent.getId(), "Max", validYearOfBirth(), "star"))
                    .isInstanceOf(InvalidChildProfileException.class)
                    .hasMessageContaining("vetted catalog");
        }
    }

    // ─── Age range validation (flow 7a) ───────────────────────────────────

    @Nested
    @DisplayName("createChildProfile() — age range validation (flow 7a)")
    class AgeRangeValidation {

        @Test
        @DisplayName("rejects year of birth corresponding to age below 7")
        void createChildProfile_withAgeBelowSeven_isRejected() {
            ParentAccount parent = verifiedParent();
            when(parents.findById(parent.getId())).thenReturn(Optional.of(parent));
            int yearForAge6 = LocalDate.now().getYear() - 6;

            assertThatThrownBy(() ->
                    service.createChildProfile(parent.getId(), "Luna", yearForAge6, "star"))
                    .isInstanceOf(InvalidChildProfileException.class)
                    .hasMessageContaining("7-12");
        }

        @Test
        @DisplayName("rejects year of birth corresponding to age above 12")
        void createChildProfile_withAgeAboveTwelve_isRejected() {
            ParentAccount parent = verifiedParent();
            when(parents.findById(parent.getId())).thenReturn(Optional.of(parent));
            int yearForAge13 = LocalDate.now().getYear() - 13;

            assertThatThrownBy(() ->
                    service.createChildProfile(parent.getId(), "Luna", yearForAge13, "star"))
                    .isInstanceOf(InvalidChildProfileException.class)
                    .hasMessageContaining("7-12");
        }
    }

    // ─── BR-003: Avatar validation ────────────────────────────────────────

    @Nested
    @DisplayName("createChildProfile() — avatar validation (BR-003)")
    class AvatarValidation {

        @Test
        @DisplayName("rejects avatar not in gender-neutral catalog (BR-003)")
        void createChildProfile_withUnknownAvatar_isRejected() {
            ParentAccount parent = verifiedParent();
            when(parents.findById(parent.getId())).thenReturn(Optional.of(parent));

            assertThatThrownBy(() ->
                    service.createChildProfile(parent.getId(), "Luna", validYearOfBirth(), "warrior"))
                    .isInstanceOf(InvalidChildProfileException.class)
                    .hasMessageContaining("gender-neutral catalog");
        }
    }

    // ─── confirmChildProfile() ────────────────────────────────────────────

    @Nested
    @DisplayName("confirmChildProfile() — secondary consent (BR-001)")
    class ConfirmChildProfile {

        @Test
        @DisplayName("activates child profile and marks parent as FULLY_CONSENTED")
        void confirmSecondConsent_marksParentFullyConsentedAndChildReady() {
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();

            ParentAccount parent = new ParentAccount(
                    parentId, "p@example.com", "hashed", "Anna", "Frau", true, true);
            parent.markEmailVerified();

            ChildProfile child = new ChildProfile(childId, "Luna", validYearOfBirth(), "star", parentId);

            VerificationToken token = new VerificationToken(
                    parentId, childId, TokenPurpose.EMAIL_SECONDARY);
            when(tokens.findById(token.getId())).thenReturn(Optional.of(token));
            when(childProfiles.findById(childId)).thenReturn(Optional.of(child));
            when(parents.findById(parentId)).thenReturn(Optional.of(parent));

            service.confirmChildProfile(token.getId());

            assertThat(child.getStatus()).isEqualTo(ChildStatus.ACTIVE);
            assertThat(parent.getStatus()).isEqualTo(ParentStatus.FULLY_CONSENTED);
        }

        @Test
        @DisplayName("throws TokenExpiredException for expired secondary token (flow 10a)")
        void confirmChildProfile_withExpiredToken_throwsTokenExpiredException() {
            UUID parentId = UUID.randomUUID();
            VerificationToken expiredToken = new VerificationToken(
                    parentId, UUID.randomUUID(), TokenPurpose.EMAIL_SECONDARY,
                    java.time.Instant.now().minusSeconds(25 * 60 * 60));
            when(tokens.findById(any())).thenReturn(Optional.of(expiredToken));

            assertThatThrownBy(() -> service.confirmChildProfile(expiredToken.getId()))
                    .isInstanceOf(TokenExpiredException.class);
        }

        @Test
        @DisplayName("writes EMAIL_SECONDARY_CONFIRMED audit entry (BR-005)")
        void confirmChildProfile_writesAuditEntry() {
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();

            ParentAccount parent = new ParentAccount(
                    parentId, "p@example.com", "hashed", "Anna", "Frau", true, true);
            parent.markEmailVerified();
            ChildProfile child = new ChildProfile(childId, "Nova", validYearOfBirth(), "moon", parentId);
            VerificationToken token = new VerificationToken(
                    parentId, childId, TokenPurpose.EMAIL_SECONDARY);

            when(tokens.findById(token.getId())).thenReturn(Optional.of(token));
            when(childProfiles.findById(childId)).thenReturn(Optional.of(child));
            when(parents.findById(parentId)).thenReturn(Optional.of(parent));

            service.confirmChildProfile(token.getId());

            ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
            verify(auditLog).save(captor.capture());
            assertThat(captor.getValue().getAction()).isEqualTo(AuditAction.EMAIL_SECONDARY_CONFIRMED);
        }
    }
}
