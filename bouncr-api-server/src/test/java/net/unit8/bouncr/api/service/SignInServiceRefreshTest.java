package net.unit8.bouncr.api.service;

import enkan.web.middleware.session.KeyValueStore;
import net.unit8.bouncr.api.resource.MockFactory;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SignInService.refreshAccessToken() — the core logic behind
 * TokenRefreshResource.
 */
class SignInServiceRefreshTest {
    private DSLContext dsl;
    private InMemoryStore bouncrTokenStore;
    private InMemoryStore refreshTokenStore;
    private StoreProvider storeProvider;
    private BouncrConfiguration config;

    @BeforeEach
    void setup() {
        dsl = MockFactory.beginTransaction();
        bouncrTokenStore = new InMemoryStore();
        refreshTokenStore = new InMemoryStore();
        storeProvider = new TestStoreProvider(bouncrTokenStore, refreshTokenStore);
        config = new BouncrConfiguration();
    }

    @AfterEach
    void tearDown() {
        MockFactory.rollback();
    }

    @Test
    void refreshAccessToken_validUser_rebuildsProfileMap() {
        // admin user (id=1) is seeded by V23 migration
        String sessionId = "test-session-123";

        SignInService service = new SignInService(dsl, storeProvider, config);
        HashMap<String, Object> profileMap = service.refreshAccessToken(1L, sessionId);

        assertThat(profileMap).isNotNull();
        assertThat(profileMap.get("sub")).isEqualTo("admin");
        assertThat(profileMap.get("iss")).isEqualTo("bouncr");
        assertThat(profileMap.get("uid")).isEqualTo("1");
        assertThat(profileMap).containsKey("permissionsByRealm");

        // Verify access token was written to store
        Serializable stored = bouncrTokenStore.read(sessionId);
        assertThat(stored).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, Object> storedMap = (Map<String, Object>) stored;
        assertThat(storedMap.get("sub")).isEqualTo("admin");
    }

    @Test
    void refreshAccessToken_validUser_extendsRefreshTokenTTL() {
        String sessionId = "test-session-456";

        SignInService service = new SignInService(dsl, storeProvider, config);
        HashMap<String, Object> profileMap = service.refreshAccessToken(1L, sessionId);

        assertThat(profileMap).isNotNull();

        // Verify refresh token marker was re-written (sliding window)
        Serializable refreshData = refreshTokenStore.read(sessionId);
        assertThat(refreshData).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, Object> refreshMap = (Map<String, Object>) refreshData;
        assertThat(refreshMap.get("userId")).isEqualTo(1L);
    }

    @Test
    void refreshAccessToken_nonExistentUser_returnsNull() {
        String sessionId = "test-session-789";

        SignInService service = new SignInService(dsl, storeProvider, config);
        HashMap<String, Object> profileMap = service.refreshAccessToken(99999L, sessionId);

        assertThat(profileMap).isNull();

        // Nothing should be written to stores
        assertThat(bouncrTokenStore.read(sessionId)).isNull();
        assertThat(refreshTokenStore.read(sessionId)).isNull();
    }

    @Test
    void refreshAccessToken_reflectsUpdatedPermissions() {
        String sessionId = "test-session-perm";

        SignInService service = new SignInService(dsl, storeProvider, config);

        // First refresh
        HashMap<String, Object> firstProfile = service.refreshAccessToken(1L, sessionId);
        assertThat(firstProfile).isNotNull();

        // Add an assignment: assign admin user (id=1) to a new role in a realm
        // This simulates permission change between refreshes
        dsl.execute("INSERT INTO roles (role_id, name, name_lower, description, write_protected) VALUES (100, 'testrole', 'testrole', 'test', false)");
        dsl.execute("INSERT INTO permissions (permission_id, name, name_lower, description, write_protected) VALUES (100, 'test:perm', 'test:perm', 'test', false)");
        dsl.execute("INSERT INTO role_permissions (role_id, permission_id) VALUES (100, 100)");
        dsl.execute("INSERT INTO realms (realm_id, name, name_lower, url, application_id, description, write_protected) " +
                "VALUES (100, 'testrealm', 'testrealm', '.*', 1, 'test', false)");
        dsl.execute("INSERT INTO assignments (group_id, role_id, realm_id) VALUES (1, 100, 100)");

        // Second refresh — should reflect new permissions
        HashMap<String, Object> secondProfile = service.refreshAccessToken(1L, sessionId);
        assertThat(secondProfile).isNotNull();
        Object secondPerms = secondProfile.get("permissionsByRealm");

        // The permissions map should now contain the new realm
        @SuppressWarnings("unchecked")
        Map<String, ?> permsMap = (Map<String, ?>) secondPerms;
        assertThat(permsMap).containsKey("100");
    }

    // --- Test helpers ---

    /**
     * Simple in-memory KeyValueStore for testing.
     */
    static class InMemoryStore implements KeyValueStore {
        private final ConcurrentHashMap<String, Serializable> map = new ConcurrentHashMap<>();

        @Override
        public Serializable read(String key) {
            return key == null ? null : map.get(key);
        }

        @Override
        public String write(String key, Serializable value) {
            if (key == null) {
                key = java.util.UUID.randomUUID().toString();
            }
            map.put(key, value);
            return key;
        }

        @Override
        public String delete(String key) {
            if (key != null) map.remove(key);
            return key;
        }
    }

    /**
     * StoreProvider subclass that returns in-memory stores without requiring
     * the full enkan component lifecycle.
     */
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
