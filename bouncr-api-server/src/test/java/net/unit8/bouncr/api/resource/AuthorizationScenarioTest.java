package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.raoh.combinator.Tuple3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Authorization scenario tests for critical resources.
 * Verifies that @Decision(AUTHORIZED) and @Decision(ALLOWED) behave correctly
 * across different principal states and permission sets.
 */
class AuthorizationScenarioTest {

    private static UserPermissionPrincipal principal(String account, String... permissions) {
        return new UserPermissionPrincipal(1L, account, Map.of(), Set.of(permissions));
    }

    // ==================== PasswordCredentialResource ====================

    @Nested
    class PasswordCredentialResourceAuth {
        private PasswordCredentialResource resource;

        @BeforeEach
        void init() {
            resource = new PasswordCredentialResource();
            BouncrConfiguration config = new BouncrConfiguration();
            config.setSecureRandom(new java.security.SecureRandom());
            config.setPbkdf2Iterations(1);
            setField(resource, "config", config);
        }

        @Test
        void authorized_nullPrincipal_rejected() {
            assertThat(resource.isAuthorized(null)).isFalse();
        }

        @Test
        void authorized_authenticatedUser_accepted() {
            assertThat(resource.isAuthorized(principal("alice"))).isTrue();
        }

        @Test
        void putAllowed_nullPrincipal_withAccount_allowedForSelfService() {
            // Unauthenticated password change is allowed when account is specified
            // (old password verification happens in the handler, not here)
            var req = new Tuple3<>("alice", "old", "new");
            assertThat(resource.isPutAllowed(null, req)).isTrue();
        }

        @Test
        void putAllowed_nullPrincipal_withoutAccount_rejected() {
            var req = new Tuple3<>((String) null, "old", "new");
            assertThat(resource.isPutAllowed(null, req)).isFalse();
        }

        @Test
        void putAllowed_sameAccount_accepted() {
            var req = new Tuple3<>("alice", "old", "new");
            assertThat(resource.isPutAllowed(principal("alice"), req)).isTrue();
        }

        @Test
        void putAllowed_differentAccount_noPermission_rejected() {
            var req = new Tuple3<>("bob", "old", "new");
            assertThat(resource.isPutAllowed(principal("alice"), req)).isFalse();
        }

        @Test
        void putAllowed_differentAccount_withAdminPermission_accepted() {
            var req = new Tuple3<>("bob", "old", "new");
            assertThat(resource.isPutAllowed(principal("alice", "any_user:update"), req)).isTrue();
        }
    }

    // ==================== OtpKeyResource ====================

    @Nested
    class OtpKeyResourceAuth {
        private OtpKeyResource resource;

        @BeforeEach
        void init() {
            resource = new OtpKeyResource();
            setField(resource, "config", new BouncrConfiguration());
        }

        @Test
        void authorized_nullPrincipal_rejected() {
            assertThat(resource.isAuthorized(null)).isFalse();
        }

        @Test
        void getAllowed_withMyRead_accepted() {
            assertThat(resource.isGetAllowed(principal("alice", "my:read"))).isTrue();
        }

        @Test
        void getAllowed_withMyUpdate_accepted() {
            assertThat(resource.isGetAllowed(principal("alice", "my:update"))).isTrue();
        }

        @Test
        void getAllowed_noPermission_rejected() {
            assertThat(resource.isGetAllowed(principal("alice"))).isFalse();
        }

        @Test
        void putDeleteAllowed_withMyUpdate_accepted() {
            assertThat(resource.allowed(principal("alice", "my:update"))).isTrue();
        }

        @Test
        void putDeleteAllowed_withOnlyMyRead_rejected() {
            assertThat(resource.allowed(principal("alice", "my:read"))).isFalse();
        }
    }

    // ==================== WebAuthnCredentialsResource ====================

    @Nested
    class WebAuthnCredentialsResourceAuth {
        private WebAuthnCredentialsResource resource;

        @BeforeEach
        void init() {
            resource = new WebAuthnCredentialsResource();
        }

        @Test
        void authorized_nullPrincipal_rejected() {
            assertThat(resource.isAuthorized(null)).isFalse();
        }

        @Test
        void getAllowed_withMyRead_accepted() {
            assertThat(resource.isGetAllowed(principal("alice", "my:read"))).isTrue();
        }

        @Test
        void getAllowed_noPermission_rejected() {
            assertThat(resource.isGetAllowed(principal("alice"))).isFalse();
        }

        @Test
        void deleteAllowed_withMyUpdate_accepted() {
            assertThat(resource.allowed(principal("alice", "my:update"))).isTrue();
        }

        @Test
        void deleteAllowed_noPermission_rejected() {
            assertThat(resource.allowed(principal("alice"))).isFalse();
        }
    }

    // ==================== UserResource ====================

    @Nested
    class UserResourceAuth {
        private UserResource resource;

        @BeforeEach
        void init() {
            resource = new UserResource();
        }

        @Test
        void authorized_nullPrincipal_rejected() {
            assertThat(resource.isAuthorized(null)).isFalse();
        }

        @Test
        void getAllowed_ownAccount_withMyRead_accepted() {
            assertThat(resource.isGetAllowed(
                    principal("alice", "my:read"), Parameters.of("account", "alice")
            )).isTrue();
        }

        @Test
        void getAllowed_otherAccount_withMyRead_rejected() {
            assertThat(resource.isGetAllowed(
                    principal("alice", "my:read"), Parameters.of("account", "bob")
            )).isFalse();
        }

        @Test
        void getAllowed_otherAccount_withUserRead_accepted() {
            assertThat(resource.isGetAllowed(
                    principal("alice", "user:read"), Parameters.of("account", "bob")
            )).isTrue();
        }

        @Test
        void getAllowed_otherAccount_withAnyUserRead_accepted() {
            assertThat(resource.isGetAllowed(
                    principal("alice", "any_user:read"), Parameters.of("account", "bob")
            )).isTrue();
        }

        @Test
        void deleteAllowed_ownAccount_withMyDelete_accepted() {
            assertThat(resource.isDeleteAllowed(
                    principal("alice", "my:delete"), Parameters.of("account", "alice")
            )).isTrue();
        }

        @Test
        void deleteAllowed_otherAccount_noAdminPermission_rejected() {
            assertThat(resource.isDeleteAllowed(
                    principal("alice", "my:delete"), Parameters.of("account", "bob")
            )).isFalse();
        }

        @Test
        void deleteAllowed_otherAccount_withAnyUserDelete_accepted() {
            assertThat(resource.isDeleteAllowed(
                    principal("alice", "any_user:delete"), Parameters.of("account", "bob")
            )).isTrue();
        }
    }

    // ==================== InvitationsResource ====================

    @Nested
    class InvitationsResourceAuth {
        private InvitationsResource resource;

        @BeforeEach
        void init() {
            resource = new InvitationsResource();
            BouncrConfiguration config = new BouncrConfiguration();
            config.setSecureRandom(new java.security.SecureRandom());
            setField(resource, "config", config);
        }

        @Test
        void authorized_nullPrincipal_rejected() {
            assertThat(resource.isAuthorized(null)).isFalse();
        }

        @Test
        void postAllowed_withInvitationCreate_accepted() {
            assertThat(resource.isPostAllowed(principal("alice", "invitation:create"))).isTrue();
        }

        @Test
        void postAllowed_noPermission_rejected() {
            assertThat(resource.isPostAllowed(principal("alice"))).isFalse();
        }

        @Test
        void getListAllowed_withInvitationCreate_accepted() {
            assertThat(resource.isGetAllowed(principal("alice", "invitation:create"))).isTrue();
        }

        @Test
        void getListAllowed_noPermission_rejected() {
            assertThat(resource.isGetAllowed(principal("alice"))).isFalse();
        }
    }

    // ==================== GroupResource ====================

    @Nested
    class GroupResourceAuth {
        private GroupResource resource;

        @BeforeEach
        void init() {
            resource = new GroupResource();
        }

        @Test
        void authorized_nullPrincipal_rejected() {
            assertThat(resource.isAuthorized(null)).isFalse();
        }

        @Test
        void deleteAllowed_withGroupDelete_accepted() {
            assertThat(resource.isDeleteAllowed(principal("alice", "group:delete"))).isTrue();
        }

        @Test
        void deleteAllowed_withAnyGroupDelete_accepted() {
            assertThat(resource.isDeleteAllowed(principal("alice", "any_group:delete"))).isTrue();
        }

        @Test
        void deleteAllowed_noPermission_rejected() {
            assertThat(resource.isDeleteAllowed(principal("alice"))).isFalse();
        }

        @Test
        void deleteAllowed_wrongPermission_anyGroupUpdate_rejected() {
            // This was a previous bug — any_group:update was used instead of any_group:delete
            assertThat(resource.isDeleteAllowed(principal("alice", "any_group:update"))).isFalse();
        }
    }

    // ==================== UserProfileVerificationResource ====================

    @Nested
    class UserProfileVerificationResourceAuth {
        private UserProfileVerificationResource resource;

        @BeforeEach
        void init() {
            resource = new UserProfileVerificationResource();
        }

        @Test
        void authorized_nullPrincipal_rejected() {
            assertThat(resource.isAuthorized(null)).isFalse();
        }

        @Test
        void authorized_authenticatedUser_accepted() {
            assertThat(resource.isAuthorized(principal("alice"))).isTrue();
        }
    }

    // ==================== OidcApplicationSecretResource ====================

    @Nested
    class OidcApplicationSecretResourceAuth {
        private OidcApplicationSecretResource resource;

        @BeforeEach
        void init() {
            resource = new OidcApplicationSecretResource();
            BouncrConfiguration config = new BouncrConfiguration();
            config.setSecureRandom(new java.security.SecureRandom());
            config.setPbkdf2Iterations(1);
            setField(resource, "config", config);
        }

        @Test
        void authorized_nullPrincipal_rejected() {
            assertThat(resource.authorized(null)).isFalse();
        }

        @Test
        void allowed_withOidcApplicationUpdate_accepted() {
            assertThat(resource.allowed(principal("alice", "oidc_application:update"))).isTrue();
        }

        @Test
        void allowed_noPermission_rejected() {
            assertThat(resource.allowed(principal("alice"))).isFalse();
        }
    }

    // ==================== Helpers ====================

    private static void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
