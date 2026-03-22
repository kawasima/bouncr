package net.unit8.bouncr.api.resource;

import enkan.data.DefaultHttpRequest;
import kotowari.restful.data.Resource;
import kotowari.restful.data.RestContext;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders.PasswordSignIn;
import net.unit8.bouncr.api.logging.ActionRecord;
import net.unit8.bouncr.api.service.AuthFailureTracker;
import net.unit8.bouncr.api.repository.UserRepository;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.data.User;
import net.unit8.bouncr.util.PasswordUtils;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for password sign-in logic using a real H2 database.
 * Tests the authentication flow: load user with credentials, verify password.
 */
public class PasswordSignInResourceTest {
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
        byte[] hash = PasswordUtils.pbkdf2("pass1234", salt, 600_000);
        userRepo.insertPasswordCredential(user.id(), hash, salt, false);

        // Simulate authentication: load user and verify password
        User loaded = userRepo.findByAccountForSignIn("kawasima").orElseThrow();
        assertThat(loaded.passwordCredential()).isNotNull();

        byte[] checkHash = PasswordUtils.pbkdf2("pass1234", loaded.passwordCredential().salt(), 600_000);
        assertThat(Arrays.equals(loaded.passwordCredential().password(), checkHash)).isTrue();
    }

    @Test
    void authenticationFailedWrongPassword() {
        UserRepository userRepo = new UserRepository(dsl);
        User user = userRepo.insert("kawasima");

        String salt = "saltsaltsaltsalt";
        byte[] hash = PasswordUtils.pbkdf2("pass1234", salt, 600_000);
        userRepo.insertPasswordCredential(user.id(), hash, salt, false);

        User loaded = userRepo.findByAccountForSignIn("kawasima").orElseThrow();
        byte[] wrongHash = PasswordUtils.pbkdf2("wrongpass", loaded.passwordCredential().salt(), 600_000);
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
        byte[] hash = PasswordUtils.pbkdf2("pass1234", salt, 600_000);
        userRepo.insertPasswordCredential(user.id(), hash, salt, true);

        User loaded = userRepo.findByAccountForSignIn("kawasima").orElseThrow();
        assertThat(loaded.passwordCredential().initial()).isTrue();

        // Password still matches even though it's initial
        byte[] checkHash = PasswordUtils.pbkdf2("pass1234", loaded.passwordCredential().salt(), 600_000);
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

        AuthFailureTracker tracker = new AuthFailureTracker() {
            { lifecycle().start(this); }
        };
        setField(tracker, "config", spyConfig);

        PasswordSignInResource resource = new PasswordSignInResource();
        setField(resource, "config", spyConfig);
        setField(resource, "storeProvider", new StoreProvider());
        setField(resource, "authFailureTracker", tracker);

        PasswordSignIn req = new PasswordSignIn("no-such-user", "anypassword", null);
        DefaultHttpRequest httpReq = new DefaultHttpRequest();
        httpReq.setRemoteAddr("127.0.0.1");
        RestContext context = restContext();
        context.put(PasswordSignInResource.SIGN_IN_REQ, req);

        boolean result = resource.authenticate(req, httpReq, new ActionRecord(), context, dsl);

        assertThat(result).isFalse();
        assertThat(dummySaltCalled[0]).as("getDummySalt() must be called for unknown accounts").isTrue();
        // No 401 Problem set — the "unknown account" branch is silent
        assertThat(context.getMessage()).isEmpty();
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
