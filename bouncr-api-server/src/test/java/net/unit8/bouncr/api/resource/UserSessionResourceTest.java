package net.unit8.bouncr.api.resource;

import enkan.data.DefaultHttpRequest;
import kotowari.restful.data.ApiResponse;
import kotowari.restful.data.Resource;
import kotowari.restful.data.RestContext;
import java.util.List;
import java.util.Map;
import net.unit8.bouncr.api.repository.OidcApplicationRepository;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.HashMap;
import java.util.Map;

import static net.unit8.bouncr.api.resource.UserSessionResource.RESOLVED_TOKEN;
import static net.unit8.bouncr.component.StoreProvider.StoreType.BOUNCR_TOKEN;
import static net.unit8.bouncr.component.StoreProvider.StoreType.REFRESH_TOKEN;
import static org.assertj.core.api.Assertions.assertThat;

class UserSessionResourceTest {
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
    void delete_returnsFrontchannelUrlsAndBackchannelSummary_evenWhenBackchannelFails() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair kp = gen.generateKeyPair();

        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        repo.insert(
                "logout-test-app",
                "logout-client",
                "hashed-secret",
                kp.getPrivate().getEncoded(),
                kp.getPublic().getEncoded(),
                "https://logout.example",
                "https://logout.example/callback",
                "desc",
                "https://example.invalid/backchannel",
                "https://logout.example/frontchannel"
        );

        StoreProvider storeProvider = new StoreProvider();
        Map<String, Object> profile = new HashMap<>();
        profile.put("sub", "admin");
        storeProvider.getStore(BOUNCR_TOKEN).write("session-token", (HashMap<String, Object>) profile);
        HashMap<String, Object> refresh = new HashMap<>();
        refresh.put("userId", 1L);
        storeProvider.getStore(REFRESH_TOKEN).write("session-token", refresh);

        UserSessionResource resource = new UserSessionResource();
        setField(resource, "storeProvider", storeProvider);
        setField(resource, "config", new BouncrConfiguration());

        RestContext context = restContext();
        context.put(RESOLVED_TOKEN, "session-token");
        resource.delete("admin", new net.unit8.bouncr.api.logging.ActionRecord(), context, dsl);
        ApiResponse apiResponse = resource.handleOk(context);
        @SuppressWarnings("unchecked")
        Map<String, Object> response = (Map<String, Object>) apiResponse.getBody();

        @SuppressWarnings("unchecked")
        List<String> frontchannelUrls = (List<String>) response.get("frontchannel_logout_urls");
        assertThat(frontchannelUrls).contains("https://logout.example/frontchannel");

        @SuppressWarnings("unchecked")
        Map<String, Object> backchannel = (Map<String, Object>) response.get("backchannel_logout");
        assertThat(backchannel.get("attempted")).isEqualTo(1);
        assertThat(backchannel.get("succeeded")).isEqualTo(0);
        assertThat(backchannel.get("failed")).isEqualTo(1);
        assertThat(storeProvider.getStore(BOUNCR_TOKEN).read("session-token")).isNull();
        assertThat(storeProvider.getStore(REFRESH_TOKEN).read("session-token")).isNull();
        // Cookie clearing header must be present
        assertThat(apiResponse.getHeaders().getList("Set-Cookie"))
                .anySatisfy(v -> assertThat(v.toString().toLowerCase()).contains("max-age=0"));
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
