package net.unit8.bouncr.api.resource;

import enkan.data.DefaultHttpRequest;
import kotowari.restful.data.Resource;
import kotowari.restful.data.RestContext;
import net.unit8.bouncr.api.logging.ActionRecord;
import net.unit8.bouncr.data.WordName;
import net.unit8.raoh.combinator.Tuple3;
import net.unit8.bouncr.component.AuthFailureTracker;
import net.unit8.bouncr.api.repository.UserRepository;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.data.User;
import net.unit8.bouncr.data.UserCredentials;
import net.unit8.bouncr.util.PasswordUtils;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import kotowari.restful.data.Problem;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for password sign-in logic using a real H2 database.
 * Tests the authentication flow: load user with credentials, verify password.
 */
public class PasswordSignInResourceTest {
    private static final int TEST_PBKDF2_ITERATIONS = 10_000;
    private DSLContext dsl;

    @BeforeEach
    void setup() {
        dsl = MockFactory.beginTransaction();
    }

    @AfterEach
    void tearDown() {
        MockFactory.rollback();
    }

    @Test
    void authenticationSuccessful() {
        UserRepository userRepo = new UserRepository(dsl);
        User user = userRepo.insert("kawasima");

        String salt = "saltsaltsaltsalt";
        byte[] hash = PasswordUtils.pbkdf2("pass1234", salt, TEST_PBKDF2_ITERATIONS);
        userRepo.insertPasswordCredential(user.id(), hash, salt, false);

        // Simulate authentication: load user and verify password
        UserCredentials loaded = userRepo.findByAccountForSignIn("kawasima").orElseThrow();
        assertThat(loaded.passwordCredential()).isNotNull();

        byte[] checkHash = PasswordUtils.pbkdf2("pass1234", loaded.passwordCredential().salt(), TEST_PBKDF2_ITERATIONS);
        assertThat(Arrays.equals(loaded.passwordCredential().password(), checkHash)).isTrue();
    }

    @Test
    void authenticationFailedWrongPassword() {
        UserRepository userRepo = new UserRepository(dsl);
        User user = userRepo.insert("kawasima");

        String salt = "saltsaltsaltsalt";
        byte[] hash = PasswordUtils.pbkdf2("pass1234", salt, TEST_PBKDF2_ITERATIONS);
        userRepo.insertPasswordCredential(user.id(), hash, salt, false);

        UserCredentials loaded = userRepo.findByAccountForSignIn("kawasima").orElseThrow();
        byte[] wrongHash = PasswordUtils.pbkdf2("wrongpass", loaded.passwordCredential().salt(), TEST_PBKDF2_ITERATIONS);
        assertThat(Arrays.equals(loaded.passwordCredential().password(), wrongHash)).isFalse();
    }

    @Test
    void authenticationFailedUserNotFound() {
        UserRepository userRepo = new UserRepository(dsl);
        assertThat(userRepo.findByAccountForSignIn("nonexistent")).isEmpty();
    }

    @Test
    void authenticationWithInitialPassword() {
        UserRepository userRepo = new UserRepository(dsl);
        User user = userRepo.insert("kawasima");

        String salt = "saltsaltsaltsalt";
        byte[] hash = PasswordUtils.pbkdf2("pass1234", salt, TEST_PBKDF2_ITERATIONS);
        userRepo.insertPasswordCredential(user.id(), hash, salt, true);

        UserCredentials loaded = userRepo.findByAccountForSignIn("kawasima").orElseThrow();
        assertThat(loaded.passwordCredential().initial()).isTrue();

        // Password still matches even though it's initial
        byte[] checkHash = PasswordUtils.pbkdf2("pass1234", loaded.passwordCredential().salt(), TEST_PBKDF2_ITERATIONS);
        assertThat(Arrays.equals(loaded.passwordCredential().password(), checkHash)).isTrue();
    }

    @Test
    void authenticate_nonExistentAccount_returnsFalseAndDummySaltIsUsed() {
        // Verify that getDummySalt() is called for unknown accounts, confirming the
        // timing-equalization PBKDF2 path is executed and guards against its removal.
        boolean[] dummySaltCalled = {false};
        BouncrConfiguration spyConfig = new BouncrConfiguration() {
            @Override
            public String getDummySalt() {
                dummySaltCalled[0] = true;
                return super.getDummySalt();
            }
        };

        AuthFailureTracker tracker = new AuthFailureTracker();
        tracker.initForTest(spyConfig);

        PasswordSignInResource resource = new PasswordSignInResource();
        setField(resource, "config", spyConfig);
        setField(resource, "storeProvider", new StoreProvider());
        setField(resource, "authFailureTracker", tracker);

        var req = new Tuple3<>(new WordName("no_such_user"), "anypassword", (String) null);
        DefaultHttpRequest httpReq = new DefaultHttpRequest();
        httpReq.setRemoteAddr("127.0.0.1");
        RestContext context = restContext();
        context.put(PasswordSignInResource.SIGN_IN_REQ, req);

        boolean result = resource.authenticate(req, null, httpReq, new ActionRecord(), context, dsl);

        assertThat(result).isFalse();
        assertThat(dummySaltCalled[0]).as("getDummySalt() must be called for unknown accounts").isTrue();
        // No 401 Problem set — the "unknown account" branch is silent
        assertThat(context.getMessage()).isEmpty();
    }

    @Test
    void authenticate_blockedIp_returns429() {
        BouncrConfiguration config = new BouncrConfiguration();
        config.setFailureIpMax(2);
        config.setFailureIpWindowSeconds(600);
        config.setFailureIpBlockSeconds(900);
        config.setFailureAccountIpMax(5);
        config.setFailureAccountIpWindowSeconds(300);
        config.setFailureAccountIpBlockSeconds(600);

        AuthFailureTracker tracker = new AuthFailureTracker();
        tracker.initForTest(config);
        // exhaust IP threshold
        tracker.recordFailure("10.0.0.1", null);
        tracker.recordFailure("10.0.0.1", null);

        PasswordSignInResource resource = new PasswordSignInResource();
        setField(resource, "config", config);
        setField(resource, "storeProvider", new StoreProvider());
        setField(resource, "authFailureTracker", tracker);

        var req = new Tuple3<>(new WordName("alice"), "anypassword", (String) null);
        DefaultHttpRequest httpReq = new DefaultHttpRequest();
        httpReq.setRemoteAddr("10.0.0.1");
        RestContext context = restContext();

        boolean result = resource.authenticate(req, null, httpReq, new ActionRecord(), context, dsl);

        assertThat(result).isFalse();
        assertThat(context.getMessage())
                .isPresent()
                .hasValueSatisfying(msg -> assertThat(((Problem) msg).getStatus()).isEqualTo(429));
    }

    private RestContext restContext() {
        Resource stubResource = decisionPoint -> ctx -> null;
        return new RestContext(stubResource, new DefaultHttpRequest());
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Class<?> clazz = target.getClass();
            while (clazz != null) {
                try {
                    var field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    field.set(target, value);
                    return;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            throw new NoSuchFieldException(fieldName);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
