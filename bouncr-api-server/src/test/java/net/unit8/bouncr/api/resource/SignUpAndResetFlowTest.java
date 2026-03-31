package net.unit8.bouncr.api.resource;

import net.unit8.bouncr.api.repository.*;
import net.unit8.bouncr.api.service.PasswordCredentialService;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.data.*;
import net.unit8.bouncr.util.PasswordUtils;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for user lifecycle flows: sign-up with invitations,
 * password reset challenges, and user lock/unlock behavior.
 */
class SignUpAndResetFlowTest {
    private DSLContext dsl;
    private BouncrConfiguration config;

    @BeforeEach
    void setup() {
        dsl = MockFactory.beginTransaction();
        config = new BouncrConfiguration();
        config.setSecureRandom(new SecureRandom());
        config.setPbkdf2Iterations(10_000);
    }

    @AfterEach
    void tearDown() {
        MockFactory.rollback();
    }

    @Nested
    class SignUpWithInvitation {
        @Test
        void signUpWithInvitationCode_userGetsGroupsFromInvitation() {
            UserRepository userRepo = new UserRepository(dsl);
            GroupRepository groupRepo = new GroupRepository(dsl);
            InvitationRepository invRepo = new InvitationRepository(dsl);

            Group group1 = groupRepo.insert(new GroupSpec(new WordName("invite_grp1"), "Group 1"));
            Group group2 = groupRepo.insert(new GroupSpec(new WordName("invite_grp2"), "Group 2"));

            Invitation invitation = invRepo.insert(
                    "user@example.com", "INVITE01", LocalDateTime.now(),
                    List.of(group1.id(), group2.id()));

            // Simulate sign-up: create user then apply invitation groups
            User user = userRepo.insert("invited_user");
            Optional<Invitation> loaded = invRepo.findByCode("INVITE01");
            assertThat(loaded).isPresent();
            assertThat(loaded.get().groupInvitations()).hasSize(2);

            for (GroupInvitation gi : loaded.get().groupInvitations()) {
                userRepo.addToGroup(user.id(), gi.group().id());
            }
            invRepo.delete("INVITE01");

            // Verify user is in both groups
            User fullUser = userRepo.findByIdFull(user.id(), true, false).orElseThrow();
            assertThat(fullUser.groups()).hasSize(2);
            assertThat(fullUser.groups().stream().map(g -> g.name().value()))
                    .containsExactlyInAnyOrder("invite_grp1", "invite_grp2");

            // Invitation should be consumed
            assertThat(invRepo.findByCode("INVITE01")).isEmpty();
        }

        @Test
        void signUpWithInvalidCode_noGroupsAssigned() {
            InvitationRepository invRepo = new InvitationRepository(dsl);
            UserRepository userRepo = new UserRepository(dsl);

            // No invitation exists for this code
            Optional<Invitation> invitation = invRepo.findByCode("BADCODE1");
            assertThat(invitation).isEmpty();

            // User is created without group assignments
            User user = userRepo.insert("no_invite_user");
            User fullUser = userRepo.findByIdFull(user.id(), true, false).orElseThrow();
            assertThat(fullUser.groups()).isEmpty();
        }

        @Test
        void invitationDeletion_cascadesGroupInvitations() {
            GroupRepository groupRepo = new GroupRepository(dsl);
            InvitationRepository invRepo = new InvitationRepository(dsl);

            Group group = groupRepo.insert(new GroupSpec(new WordName("gi_cascade_grp"), "Group"));
            invRepo.insert("cascade@test.com", "CASCDE01", LocalDateTime.now(),
                    List.of(group.id()));

            // Verify group_invitations exist
            Optional<Invitation> loaded = invRepo.findByCode("CASCDE01");
            assertThat(loaded).isPresent();
            assertThat(loaded.get().groupInvitations()).hasSize(1);

            // Delete invitation - group_invitations should cascade
            invRepo.delete("CASCDE01");
            assertThat(invRepo.findByCode("CASCDE01")).isEmpty();
        }

        @Test
        void signUpWithPasswordInitialization_generatesInitialPassword() {
            UserRepository userRepo = new UserRepository(dsl);
            PasswordCredentialService passwordService = new PasswordCredentialService(dsl, config);

            User user = userRepo.insert("newuser");
            InitialPassword initial = passwordService.initializePassword(user);

            assertThat(initial).isNotNull();
            assertThat(initial.password()).isNotNull();
            assertThat(initial.password()).hasSizeGreaterThanOrEqualTo(8);

            // Verify password credential was stored and is marked as initial
            UserCredentials creds = userRepo.findByAccountForSignIn("newuser").orElseThrow();
            assertThat(creds.passwordCredential()).isNotNull();
            assertThat(creds.passwordCredential().initial()).isTrue();

            // Verify we can authenticate with the generated password
            byte[] checkHash = PasswordUtils.pbkdf2(
                    initial.password(),
                    creds.passwordCredential().salt(),
                    config.getPbkdf2Iterations());
            assertThat(creds.passwordCredential().password()).isEqualTo(checkHash);
        }

        @Test
        void duplicateAccountName_isDetectedByUniquenessCheck() {
            UserRepository userRepo = new UserRepository(dsl);
            userRepo.insert("existing_user");

            assertThat(userRepo.isAccountUnique("existing_user")).isFalse();
            assertThat(userRepo.isAccountUnique("EXISTING_USER")).isFalse();
            assertThat(userRepo.isAccountUnique("new_user")).isTrue();
        }
    }

    @Nested
    class PasswordResetFlow {
        @Test
        void createChallenge_findByCode_resetPassword() {
            UserRepository userRepo = new UserRepository(dsl);
            PasswordResetChallengeRepository challengeRepo = new PasswordResetChallengeRepository(dsl);
            PasswordCredentialService passwordService = new PasswordCredentialService(dsl, config);

            // Setup: create user with password
            User user = userRepo.insert("reset_user");
            String origSalt = "originalsaltsalt";
            byte[] origHash = PasswordUtils.pbkdf2("oldpassword", origSalt, config.getPbkdf2Iterations());
            userRepo.insertPasswordCredential(user.id(), origHash, origSalt, false);

            // Create challenge
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(2);
            PasswordResetChallenge challenge = challengeRepo.insert(user.id(), "RESET001", expiresAt);
            assertThat(challenge.id()).isNotNull();
            assertThat(challenge.code()).isEqualTo("RESET001");

            // Find the challenge by code
            Optional<PasswordResetChallenge> found = challengeRepo.findByCode("RESET001");
            assertThat(found).isPresent();
            assertThat(found.get().user()).isNotNull();
            assertThat(found.get().user().account()).isEqualTo("reset_user");

            // Find active challenge (not expired)
            Optional<PasswordResetChallenge> active = challengeRepo.findActiveByCode("RESET001");
            assertThat(active).isPresent();

            // Reset password: delete challenge, generate new password
            challengeRepo.delete(challenge.id());
            InitialPassword newPassword = passwordService.initializePassword(user);

            // Old password should no longer work
            UserCredentials reloaded = userRepo.findByAccountForSignIn("reset_user").orElseThrow();
            byte[] oldCheck = PasswordUtils.pbkdf2("oldpassword", reloaded.passwordCredential().salt(), config.getPbkdf2Iterations());
            assertThat(reloaded.passwordCredential().password()).isNotEqualTo(oldCheck);

            // New password should work
            byte[] newCheck = PasswordUtils.pbkdf2(
                    newPassword.password(),
                    reloaded.passwordCredential().salt(),
                    config.getPbkdf2Iterations());
            assertThat(reloaded.passwordCredential().password()).isEqualTo(newCheck);

            // Challenge should be consumed
            assertThat(challengeRepo.findByCode("RESET001")).isEmpty();
        }

        @Test
        void expiredChallenge_notFoundByFindActive() {
            UserRepository userRepo = new UserRepository(dsl);
            PasswordResetChallengeRepository challengeRepo = new PasswordResetChallengeRepository(dsl);

            User user = userRepo.insert("expired_user");
            // Create challenge that already expired
            LocalDateTime expired = LocalDateTime.now().minusHours(1);
            challengeRepo.insert(user.id(), "EXPIRED1", expired);

            // findByCode still returns it (raw lookup)
            assertThat(challengeRepo.findByCode("EXPIRED1")).isPresent();

            // findActiveByCode filters by expiry
            assertThat(challengeRepo.findActiveByCode("EXPIRED1")).isEmpty();
        }

        @Test
        void wrongCode_notFound() {
            PasswordResetChallengeRepository challengeRepo = new PasswordResetChallengeRepository(dsl);

            assertThat(challengeRepo.findByCode("NONEXIST")).isEmpty();
            assertThat(challengeRepo.findActiveByCode("NONEXIST")).isEmpty();
        }

        @Test
        void multipleChallenge_forSameUser_allAccessible() {
            UserRepository userRepo = new UserRepository(dsl);
            PasswordResetChallengeRepository challengeRepo = new PasswordResetChallengeRepository(dsl);

            User user = userRepo.insert("multi_challenge_user");
            LocalDateTime future = LocalDateTime.now().plusHours(2);
            challengeRepo.insert(user.id(), "MULTI_01", future);
            challengeRepo.insert(user.id(), "MULTI_02", future);

            // Both should be found
            assertThat(challengeRepo.findActiveByCode("MULTI_01")).isPresent();
            assertThat(challengeRepo.findActiveByCode("MULTI_02")).isPresent();
        }

        @Test
        void changePassword_updatesCredentials() {
            UserRepository userRepo = new UserRepository(dsl);
            PasswordCredentialService passwordService = new PasswordCredentialService(dsl, config);

            User user = userRepo.insert("change_pw_user");
            String origSalt = "originalsaltsalt";
            byte[] origHash = PasswordUtils.pbkdf2("original", origSalt, config.getPbkdf2Iterations());
            userRepo.insertPasswordCredential(user.id(), origHash, origSalt, false);

            // Change password
            passwordService.changePassword(user, "newpassword123");

            // Verify old password no longer works
            UserCredentials creds = userRepo.findByAccountForSignIn("change_pw_user").orElseThrow();
            byte[] oldCheck = PasswordUtils.pbkdf2("original", creds.passwordCredential().salt(), config.getPbkdf2Iterations());
            assertThat(creds.passwordCredential().password()).isNotEqualTo(oldCheck);

            // Verify new password works
            byte[] newCheck = PasswordUtils.pbkdf2("newpassword123", creds.passwordCredential().salt(), config.getPbkdf2Iterations());
            assertThat(creds.passwordCredential().password()).isEqualTo(newCheck);

            // After changePassword, initial flag should be false
            assertThat(creds.passwordCredential().initial()).isFalse();
        }
    }

    @Nested
    class UserLockFlow {
        @Test
        void lockUser_andVerifyLocked() {
            UserRepository userRepo = new UserRepository(dsl);

            User user = userRepo.insert("lockable_user");
            assertThat(userRepo.isLocked(user.id())).isFalse();

            userRepo.lockUser(user.id(), LockLevel.LOOSE);
            assertThat(userRepo.isLocked(user.id())).isTrue();

            // Verify lock appears in sign-in data
            UserCredentials creds = userRepo.findByAccountForSignIn("lockable_user").orElseThrow();
            assertThat(creds.userLock()).isNotNull();
            assertThat(creds.userLock().lockLevel()).isEqualTo(LockLevel.LOOSE);
        }

        @Test
        void unlockUser_removesLock() {
            UserRepository userRepo = new UserRepository(dsl);

            User user = userRepo.insert("unlock_user");
            userRepo.lockUser(user.id(), LockLevel.LOOSE);
            assertThat(userRepo.isLocked(user.id())).isTrue();

            userRepo.unlockUser(user.id());
            assertThat(userRepo.isLocked(user.id())).isFalse();

            UserCredentials creds = userRepo.findByAccountForSignIn("unlock_user").orElseThrow();
            assertThat(creds.userLock()).isNull();
        }

        @Test
        void banUser_lockLevelIsBan() {
            UserRepository userRepo = new UserRepository(dsl);

            User user = userRepo.insert("banned_user");
            userRepo.lockUser(user.id(), LockLevel.BAN);

            UserCredentials creds = userRepo.findByAccountForSignIn("banned_user").orElseThrow();
            assertThat(creds.userLock()).isNotNull();
            assertThat(creds.userLock().lockLevel()).isEqualTo(LockLevel.BAN);
        }

        @Test
        void lockUser_thenUpgradeToBan() {
            UserRepository userRepo = new UserRepository(dsl);

            User user = userRepo.insert("upgrade_lock_user");
            userRepo.lockUser(user.id(), LockLevel.LOOSE);
            // Upgrade to BAN (upsert behavior)
            userRepo.lockUser(user.id(), LockLevel.BAN);

            UserCredentials creds = userRepo.findByAccountForSignIn("upgrade_lock_user").orElseThrow();
            assertThat(creds.userLock().lockLevel()).isEqualTo(LockLevel.BAN);
        }

        @Test
        void unlockNonLockedUser_doesNotThrow() {
            UserRepository userRepo = new UserRepository(dsl);
            User user = userRepo.insert("not_locked");
            userRepo.unlockUser(user.id()); // should not throw
            assertThat(userRepo.isLocked(user.id())).isFalse();
        }

        @Test
        void deleteUser_cascadesLock() {
            UserRepository userRepo = new UserRepository(dsl);
            User user = userRepo.insert("cascade_lock_user");
            userRepo.lockUser(user.id(), LockLevel.BAN);
            assertThat(userRepo.isLocked(user.id())).isTrue();

            userRepo.delete(user.id());
            assertThat(userRepo.findByAccount("cascade_lock_user")).isEmpty();
        }
    }

    @Nested
    class OtpKeyFlow {
        @Test
        void insertAndFindOtpKey() {
            UserRepository userRepo = new UserRepository(dsl);
            User user = userRepo.insert("otp_user");

            byte[] key = new byte[20];
            new SecureRandom().nextBytes(key);
            userRepo.insertOtpKey(user.id(), key);

            Optional<OtpKey> found = userRepo.findOtpKey(user.id());
            assertThat(found).isPresent();
            assertThat(found.get().key()).isEqualTo(key);
        }

        @Test
        void otpKeyAppearsInSignInCredentials() {
            UserRepository userRepo = new UserRepository(dsl);
            User user = userRepo.insert("otp_signin_user");

            byte[] key = new byte[20];
            new SecureRandom().nextBytes(key);
            userRepo.insertOtpKey(user.id(), key);

            UserCredentials creds = userRepo.findByAccountForSignIn("otp_signin_user").orElseThrow();
            assertThat(creds.otpKey()).isNotNull();
            assertThat(creds.otpKey().key()).isEqualTo(key);
        }

        @Test
        void deleteOtpKey_removesIt() {
            UserRepository userRepo = new UserRepository(dsl);
            User user = userRepo.insert("otp_del_user");

            byte[] key = new byte[20];
            new SecureRandom().nextBytes(key);
            userRepo.insertOtpKey(user.id(), key);
            assertThat(userRepo.findOtpKey(user.id())).isPresent();

            userRepo.deleteOtpKey(user.id());
            assertThat(userRepo.findOtpKey(user.id())).isEmpty();

            UserCredentials creds = userRepo.findByAccountForSignIn("otp_del_user").orElseThrow();
            assertThat(creds.otpKey()).isNull();
        }

        @Test
        void upsertOtpKey_replacesExisting() {
            UserRepository userRepo = new UserRepository(dsl);
            User user = userRepo.insert("otp_upsert_user");

            byte[] key1 = new byte[20];
            Arrays.fill(key1, (byte) 0x01);
            userRepo.insertOtpKey(user.id(), key1);

            byte[] key2 = new byte[20];
            Arrays.fill(key2, (byte) 0x02);
            userRepo.insertOtpKey(user.id(), key2);

            Optional<OtpKey> found = userRepo.findOtpKey(user.id());
            assertThat(found).isPresent();
            assertThat(found.get().key()).isEqualTo(key2);
        }
    }
}
