package net.unit8.bouncr.sign;

import enkan.web.jwt.JwtHeader;
import enkan.web.jwt.JwtProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonWebTokenTest {
    private static final byte[] SECRET = "abcdefghijklmnopqrstuvwxyzabcdef".getBytes(StandardCharsets.UTF_8);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private JsonWebToken jwt;

    @BeforeEach
    void setUp() {
        jwt = new JsonWebToken();
        jwt.lifecycle().start(jwt);
    }

    @Test
    void sign_producesThreePartJwt() {
        Map<String, Object> payload = Map.of("sub", "testuser");
        JwtHeader header = new JwtHeader("HS256");

        String token = jwt.sign(payload, header, SECRET);

        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3);
    }

    @Test
    void signAndDecode_roundTrip() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", "alice");
        payload.put("iss", "bouncr");
        payload.put("admin", true);
        JwtHeader header = new JwtHeader("HS256");

        String token = jwt.sign(payload, header, SECRET);
        String payloadB64 = token.split("\\.")[1];

        Map<String, Object> decoded = jwt.decodePayload(payloadB64, MAP_TYPE);
        assertThat(decoded).containsEntry("sub", "alice");
        assertThat(decoded).containsEntry("iss", "bouncr");
        assertThat(decoded).containsEntry("admin", true);
    }

    @Test
    void sign_headerContainsAlg() {
        Map<String, Object> payload = Map.of("sub", "test");
        JwtHeader header = new JwtHeader("HS256");

        String token = jwt.sign(payload, header, SECRET);
        String headerJson = new String(Base64.getUrlDecoder().decode(token.split("\\.")[0]));

        assertThat(headerJson).contains("\"alg\":\"HS256\"");
        assertThat(headerJson).contains("\"typ\":\"JWT\"");
    }

    @Test
    void sign_signatureIsVerifiable() {
        Map<String, Object> payload = Map.of("sub", "testuser");
        JwtHeader header = new JwtHeader("HS256");

        String token = jwt.sign(payload, header, SECRET);
        Key key = new SecretKeySpec(SECRET, "HmacSHA256");

        byte[] verified = JwtProcessor.verify(token, key);
        assertThat(verified).isNotNull();
    }

    @Test
    void decodePayload_invalidBase64_throwsException() {
        assertThatThrownBy(() -> jwt.decodePayload("!!!invalid!!!", MAP_TYPE))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to decode JWT payload");
    }

    @Test
    void decodePayload_invalidJson_throwsException() {
        String b64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("not json".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> jwt.decodePayload(b64, MAP_TYPE))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to decode JWT payload");
    }
}
