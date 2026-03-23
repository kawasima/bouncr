package net.unit8.bouncr.api.resource;

import enkan.middleware.session.KeyValueStore;
import net.unit8.bouncr.api.repository.UserRepository;
import net.unit8.bouncr.api.repository.UserSessionRepository;
import net.unit8.bouncr.api.service.SignInService;
import net.unit8.bouncr.api.service.SignInService.PasswordCredentialStatus;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.component.config.PasswordPolicy;
import net.unit8.bouncr.data.*;
import net.unit8.bouncr.util.PasswordUtils;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for user session management and token refresh flow.
 * Covers UserSessionRepository CRUD, cascade semantics, duplicate token
 * constraints, and SignInService password credential validation paths.
 */
class TokenRefreshTest {
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
    class UserSessionCrud {
        @Test
        void insertAndFindByToken() {
            UserRepository userRepo = new UserRepository(dsl);
            UserSessionRepository sessionRepo = new UserSessionRepository(dsl);

            User user = userRepo.insert("session_user");
            UserSession session = sessionRepo.insert(
                    user.id(), "tok-abc-123", "192.168.1.1", "TestAgent/1.0", LocalDateTime.now());

            assertThat(session.id()).isNotNull();
            assertThat(session.token()).isEqualTo("tok-abc-123");

            Optional<UserSession> found = sessionRepo.findByToken("tok-abc-123");
            assertThat(found).isPresent();
            assertThat(found.get().user()).isNotNull();
            assertThat(found.get().user().account()).isEqualTo("session_user");
            assertThat(found.get().remoteAddress()).isEqualTo("192.168.1.1");
            assertThat(found.get().userAgent()).isEqualTo("TestAgent/1.0");
        }

        @Test
        void findByToken_nonExistent_returnsEmpty() {
            UserSessionRepository sessionRepo = new UserSessionRepository(dsl);
            assertThat(sessionRepo.findByToken("nonexistent-token")).isEmpty();
        }

        @Test
        void deleteByToken_removesSession() {
            UserRepository userRepo = new UserRepository(dsl);
            UserSessionRepository sessionRepo = new UserSessionRepository(dsl);

            User user = userRepo.insert("del_session_user");
            sessionRepo.insert(user.id(), "tok-del-001", "10.0.0.1", "Agent", LocalDateTime.now());

            assertThat(sessionRepo.findByToken("tok-del-001")).isPresent();
            sessionRepo.deleteByToken("tok-del-001");
            assertThat(sessionRepo.findByToken("tok-del-001")).isEmpty();
        }

        @Test
        void deleteByToken_nonExistent_doesNotThrow() {
            UserSessionRepository sessionRepo = new UserSessionRepository(dsl);
            sessionRepo.deleteByToken("no-such-token"); // should not throw
        }

        @Test
        void duplicateToken_throwsConstraintViolation() {
            UserRepository userRepo = new UserRepository(dsl);
            UserSessionRepository sessionRepo = new UserSessionRepository(dsl);

            User user = userRepo.insert("dup_tok_user");
            sessionRepo.insert(user.id(), "dup-token", "10.0.0.1", "Agent", LocalDateTime.now());
            assertThatThrownBy(() ->
                    sessionRepo.insert(user.id(), "dup-token", "10.0.0.2", "Agent2", LocalDateTime.now()))
                    .isInstanceOf(DataAccessException.class);
        }

        @Test
        void searchByUserId_returnsAllSessions() {
            UserRepository userRepo = new UserRepository(dsl);
            UserSessionRepository sessionRepo = new UserSessionRepository(dsl);

            User user = userRepo.insert("multi_sess_user");
            sessionRepo.insert(user.id(), "tok-s1", "10.0.0.1", "Chrome", LocalDateTime.now());
            sessionRepo.insert(user.id(), "tok-s2", "10.0.0.2", "Firefox", LocalDateTime.now());
            sessionRepo.insert(user.id(), "tok-s3", "10.0.0.3", "Safari", LocalDateTime.now());

            // Another user's session should not appear
            User other = userRepo.insert("other_sess_user");
            sessionRepo.insert(other.id(), "tok-other", "10.0.0.4", "Edge", LocalDateTime.now());

            List<UserSession> sessions = sessionRepo.searchByUserId(user.id(), 0, 100);
            assertThat(sessions).hasSize(3);
            assertThat(sessions.stream().map(UserSession::token))
                    .containsExactlyInAnyOrder("tok-s1", "tok-s2", "tok-s3");
        }

        @Test
        void searchByUserId_pagination() {
            UserRepository userRepo = new UserRepository(dsl);
            UserSessionRepository sessionRepo = new UserSessionRepository(dsl);

            User user = userRepo.insert("paged_user");
            for (int i = 0; i < 5; i++) {
                sessionRepo.insert(user.id(), "tok-page-" + i, "10.0.0." + i, "Agent", LocalDateTime.now());
            }

            List<UserSession> page1 = sessionRepo.searchByUserId(user.id(), 0, 2);
            assertThat(page1).hasSize(2);

            List<UserSession> page2 = sessionRepo.searchByUserId(user.id(), 2, 2);
            assertThat(page2).hasSize(2);

            List<UserSession> page3 = sessionRepo.searchByUserId(user.id(), 4, 2);
            assertThat(page3).hasSize(1);
        }

        @Test
        void deleteUser_cascadesSessions() {
            UserRepository userRepo = new UserRepository(dsl);
            UserSessionRepository sessionRepo = new UserSessionRepository(dsl);

            User user = userRepo.insert("cascade_sess_user");
            sessionRepo.insert(user.id(), "tok-cascade", "10.0.0.1", "Agent", LocalDateTime.now());

            assertThat(sessionRepo.findByToken("tok-cascade")).isPresent();

            userRepo.delete(user.id());
            // Session should be gone due to ON DELETE CASCADE on user_id FK
            assertThat(sessionRepo.findByToken("tok-cascade")).isEmpty();
        }
    }

    @Nested
    class PasswordCredentialValidation {
        @Test
        void validNonInitialNonExpired_returnsValid() {
            UserRepository userRepo = new UserRepository(dsl);
            User user = userRepo.insert("valid_cred_user");
            String salt = "saltsaltsaltsalt";
            byte[] hash = PasswordUtils.pbkdf2("pass1234", salt, config.getPbkdf2Iterations());
            userRepo.insertPasswordCredential(user.id(), hash, salt, false);

            UserCredentials creds = userRepo.findByAccountForSignIn("valid_cred_user").orElseThrow();

            InMemoryStore bouncrStore = new InMemoryStore();
            InMemoryStore refreshStore = new InMemoryStore();
            StoreProvider sp = new TestStoreProvider(bouncrStore, refreshStore);
            SignInService service = new SignInService(dsl, sp, config);

            PasswordCredentialStatus status = service.validatePasswordCredentialAttributes(creds);
            assertThat(status).isEqualTo(PasswordCredentialStatus.VALID);
        }

        @Test
        void initialPassword_returnsInitial() {
            UserRepository userRepo = new UserRepository(dsl);
            User user = userRepo.insert("initial_cred_user");
            String salt = "saltsaltsaltsalt";
            byte[] hash = PasswordUtils.pbkdf2("temp1234", salt, config.getPbkdf2Iterations());
            userRepo.insertPasswordCredential(user.id(), hash, salt, true);

            UserCredentials creds = userRepo.findByAccountForSignIn("initial_cred_user").orElseThrow();

            InMemoryStore bouncrStore = new InMemoryStore();
            InMemoryStore refreshStore = new InMemoryStore();
            StoreProvider sp = new TestStoreProvider(bouncrStore, refreshStore);
            SignInService service = new SignInService(dsl, sp, config);

            PasswordCredentialStatus status = service.validatePasswordCredentialAttributes(creds);
            assertThat(status).isEqualTo(PasswordCredentialStatus.INITIAL);
        }

        @Test
        void expiredPassword_returnsExpired() {
            UserRepository userRepo = new UserRepository(dsl);
            User user = userRepo.insert("expired_cred_user");
            String salt = "saltsaltsaltsalt";
            byte[] hash = PasswordUtils.pbkdf2("pass1234", salt, config.getPbkdf2Iterations());
            userRepo.insertPasswordCredential(user.id(), hash, salt, false);

            // Backdate the created_at to simulate expiration
            dsl.update(org.jooq.impl.DSL.table("password_credentials"))
                    .set(org.jooq.impl.DSL.field("created_at"), LocalDateTime.now().minusDays(100))
                    .where(org.jooq.impl.DSL.field("user_id").eq(user.id()))
                    .execute();

            // Configure password policy with short expiry
            PasswordPolicy policy = new PasswordPolicy();
            policy.setExpires(Duration.ofDays(30));
            config.setPasswordPolicy(policy);

            UserCredentials creds = userRepo.findByAccountForSignIn("expired_cred_user").orElseThrow();

            InMemoryStore bouncrStore = new InMemoryStore();
            InMemoryStore refreshStore = new InMemoryStore();
            StoreProvider sp = new TestStoreProvider(bouncrStore, refreshStore);
            SignInService service = new SignInService(dsl, sp, config);

            PasswordCredentialStatus status = service.validatePasswordCredentialAttributes(creds);
            assertThat(status).isEqualTo(PasswordCredentialStatus.EXPIRED);
        }

        @Test
        void noExpiryConfigured_alwaysValid() {
            UserRepository userRepo = new UserRepository(dsl);
            User user = userRepo.insert("no_expiry_user");
            String salt = "saltsaltsaltsalt";
            byte[] hash = PasswordUtils.pbkdf2("pass1234", salt, config.getPbkdf2Iterations());
            userRepo.insertPasswordCredential(user.id(), hash, salt, false);

            // Backdate the credential
            dsl.update(org.jooq.impl.DSL.table("password_credentials"))
                    .set(org.jooq.impl.DSL.field("created_at"), LocalDateTime.now().minusYears(5))
                    .where(org.jooq.impl.DSL.field("user_id").eq(user.id()))
                    .execute();

            // Default policy has no expiry
            UserCredentials creds = userRepo.findByAccountForSignIn("no_expiry_user").orElseThrow();

            InMemoryStore bouncrStore = new InMemoryStore();
            InMemoryStore refreshStore = new InMemoryStore();
            StoreProvider sp = new TestStoreProvider(bouncrStore, refreshStore);
            SignInService service = new SignInService(dsl, sp, config);

            PasswordCredentialStatus status = service.validatePasswordCredentialAttributes(creds);
            assertThat(status).isEqualTo(PasswordCredentialStatus.VALID);
        }
    }

    @Nested
    class RefreshAccessTokenWithSessions {
        @Test
        void refreshAfterUserDeleted_returnsNull() {
            UserRepository userRepo = new UserRepository(dsl);
            User user = userRepo.insert("refresh_del_user");

            InMemoryStore bouncrStore = new InMemoryStore();
            InMemoryStore refreshStore = new InMemoryStore();
            StoreProvider sp = new TestStoreProvider(bouncrStore, refreshStore);
            SignInService service = new SignInService(dsl, sp, config);

            // Simulate refresh data in store
            HashMap<String, Object> refreshData = new HashMap<>();
            refreshData.put("userId", user.id());
            refreshStore.write("session-x", refreshData);

            // Delete the user
            userRepo.delete(user.id());

            // Refresh should fail gracefully
            HashMap<String, Object> result = service.refreshAccessToken(user.id(), "session-x");
            assertThat(result).isNull();
        }

        @Test
        void refreshWithLockedUser_returnsNullAndDeletesTokens() {
            UserRepository userRepo = new UserRepository(dsl);
            User user = userRepo.insert("locked_refresh_user");
            userRepo.lockUser(user.id(), LockLevel.BAN);

            InMemoryStore bouncrStore = new InMemoryStore();
            InMemoryStore refreshStore = new InMemoryStore();
            // Pre-populate tokens to verify they get deleted
            bouncrStore.write("locked-session", new java.util.HashMap<>(java.util.Map.of("sub", "locked_refresh_user")));
            refreshStore.write("locked-session", new java.util.HashMap<>(java.util.Map.of("userId", user.id())));

            StoreProvider sp = new TestStoreProvider(bouncrStore, refreshStore);
            SignInService service = new SignInService(dsl, sp, config);

            HashMap<String, Object> result = service.refreshAccessToken(user.id(), "locked-session");
            assertThat(result).isNull();
            // Tokens should be cleaned up
            assertThat(bouncrStore.read("locked-session")).isNull();
            assertThat(refreshStore.read("locked-session")).isNull();
        }

        @Test
        void multipleSessionsForSameUser_independentlyRefreshable() {
            InMemoryStore bouncrStore = new InMemoryStore();
            InMemoryStore refreshStore = new InMemoryStore();
            StoreProvider sp = new TestStoreProvider(bouncrStore, refreshStore);
            SignInService service = new SignInService(dsl, sp, config);

            // Use admin user from seed data
            HashMap<String, Object> prof1 = service.refreshAccessToken(1L, "session-1");
            HashMap<String, Object> prof2 = service.refreshAccessToken(1L, "session-2");

            assertThat(prof1).isNotNull();
            assertThat(prof2).isNotNull();

            // Both sessions should have independent store entries
            assertThat(bouncrStore.read("session-1")).isNotNull();
            assertThat(bouncrStore.read("session-2")).isNotNull();

            // Deleting one session's store entry should not affect the other
            bouncrStore.delete("session-1");
            assertThat(bouncrStore.read("session-1")).isNull();
            assertThat(bouncrStore.read("session-2")).isNotNull();
        }
    }

    // --- Test helpers ---

    static class InMemoryStore implements KeyValueStore {
        private final ConcurrentHashMap<String, Serializable> map = new ConcurrentHashMap<>();

        @Override
        public Serializable read(String key) {
            return key == null ? null : map.get(key);
        }

        @Override
        public String write(String key, Serializable value) {
            if (key == null) key = java.util.UUID.randomUUID().toString();
            map.put(key, value);
            return key;
        }

        @Override
        public String delete(String key) {
            if (key != null) map.remove(key);
            return key;
        }
    }

    static class TestStoreProvider extends StoreProvider {
        private final KeyValueStore bouncrToken;
        private final KeyValueStore refreshToken;

        TestStoreProvider(KeyValueStore bouncrToken, KeyValueStore refreshToken) {
            this.bouncrToken = bouncrToken;
            this.refreshToken = refreshToken;
        }

        @Override
        public KeyValueStore getStore(StoreType storeType) {
            return switch (storeType) {
                case BOUNCR_TOKEN -> bouncrToken;
                case REFRESH_TOKEN -> refreshToken;
                default -> throw new UnsupportedOperationException("Store " + storeType + " not configured in test");
            };
        }
    }
}
