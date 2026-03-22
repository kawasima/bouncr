package net.unit8.bouncr.api.service;

import net.unit8.bouncr.api.repository.OidcApplicationRepository;
import net.unit8.bouncr.api.resource.MockFactory;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.data.OidcApplication;
import net.unit8.bouncr.sign.RsaJwtSigner;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OidcLogoutServiceTest {
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
    void createLogoutToken_containsRequiredClaims() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair keyPair = gen.generateKeyPair();

        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        OidcApplication app = repo.insert(
                "oidc-app-logout",
                "client-logout",
                "secret",
                keyPair.getPrivate().getEncoded(),
                keyPair.getPublic().getEncoded(),
                "https://client.example",
                "https://client.example/callback",
                "desc",
                "https://client.example/backchannel-logout",
                "https://client.example/frontchannel-logout"
        );

        BouncrConfiguration config = new BouncrConfiguration();
        config.setIssuerBaseUrl("https://issuer.example");
        config.setClock(Clock.fixed(Instant.ofEpochSecond(1700000000L), ZoneOffset.UTC));

        OidcLogoutService service = new OidcLogoutService(config);
        String logoutToken = service.createLogoutToken(app, "admin");

        Map<String, Object> claims = RsaJwtSigner.verify(logoutToken, keyPair.getPublic().getEncoded());
        assertThat(claims).isNotNull();
        assertThat(claims.get("iss")).isEqualTo("https://issuer.example");
        assertThat(claims.get("aud")).isEqualTo("client-logout");
        assertThat(claims.get("sub")).isEqualTo("admin");
        assertThat(claims.get("jti")).isNotNull();
        assertThat(((Number) claims.get("iat")).longValue()).isEqualTo(1700000000L);
        @SuppressWarnings("unchecked")
        Map<String, Object> events = (Map<String, Object>) claims.get("events");
        assertThat(events).containsKey("http://schemas.openid.net/event/backchannel-logout");
    }
}
