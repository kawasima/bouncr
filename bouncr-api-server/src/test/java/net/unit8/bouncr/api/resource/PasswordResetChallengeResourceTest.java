package net.unit8.bouncr.api.resource;

import enkan.data.DefaultHttpRequest;
import kotowari.restful.data.Resource;
import kotowari.restful.data.RestContext;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.data.WordName;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordResetChallengeResourceTest {
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
    void create_existingAccount_createsChallenge() {
        PasswordResetChallengeResource resource = new PasswordResetChallengeResource();
        setField(resource, "config", new BouncrConfiguration());

        RestContext context = restContext();
        WordName req = new WordName("admin");
        context.put(PasswordResetChallengeResource.CREATE_REQ, req);

        resource.create(req, context, dsl);

        assertThat(context.get(PasswordResetChallengeResource.CHALLENGE)).isPresent();
    }

    @Test
    void create_nonExistentAccount_returnsSilentlyWithNoChallenge() {
        PasswordResetChallengeResource resource = new PasswordResetChallengeResource();
        setField(resource, "config", new BouncrConfiguration());

        RestContext context = restContext();
        WordName req = new WordName("no-such-user");
        context.put(PasswordResetChallengeResource.CREATE_REQ, req);

        resource.create(req, context, dsl);

        // No challenge created, no exception — response is indistinguishable from success
        assertThat(context.get(PasswordResetChallengeResource.CHALLENGE)).isEmpty();
    }

    private RestContext restContext() {
        Resource stubResource = decisionPoint -> ctx -> null;
        return new RestContext(stubResource, new DefaultHttpRequest());
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
