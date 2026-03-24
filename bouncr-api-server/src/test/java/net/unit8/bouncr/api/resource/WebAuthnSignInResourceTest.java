package net.unit8.bouncr.api.resource;

import enkan.data.DefaultHttpRequest;
import kotowari.restful.data.Problem;
import kotowari.restful.data.Resource;
import kotowari.restful.data.RestContext;
import net.unit8.bouncr.api.boundary.WebAuthnAuthenticate;
import net.unit8.bouncr.api.logging.ActionRecord;
import net.unit8.bouncr.component.AuthFailureTracker;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class WebAuthnSignInResourceTest {

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
        tracker.recordFailure("10.0.0.1", null);
        tracker.recordFailure("10.0.0.1", null);

        WebAuthnSignInResource resource = new WebAuthnSignInResource();
        setField(resource, "config", config);
        setField(resource, "storeProvider", new StoreProvider());
        setField(resource, "authFailureTracker", tracker);

        WebAuthnAuthenticate req = new WebAuthnAuthenticate("{}");
        DefaultHttpRequest httpReq = new DefaultHttpRequest();
        httpReq.setRemoteAddr("10.0.0.1");
        RestContext context = restContext();

        boolean result = resource.authenticate(req, httpReq, new ActionRecord(), context, null);

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
                    Field field = clazz.getDeclaredField(fieldName);
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
