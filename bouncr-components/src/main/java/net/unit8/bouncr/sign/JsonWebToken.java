package net.unit8.bouncr.sign;

import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import enkan.web.jwt.JwtHeader;
import enkan.web.jwt.JwtProcessor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;
import java.util.Map;

/**
 * A SystemComponent that provides JWT signing and payload decoding.
 */
public class JsonWebToken extends SystemComponent<JsonWebToken> {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Override
    protected ComponentLifecycle<JsonWebToken> lifecycle() {
        return new ComponentLifecycle<>() {
            @Override
            public void start(JsonWebToken component) {}

            @Override
            public void stop(JsonWebToken component) {}
        };
    }

    /**
     * Decode a base64url-encoded JWT payload segment into the given type.
     */
    public <T> T decodePayload(String payloadB64, TypeReference<T> type) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(payloadB64);
            return JSON.readValue(bytes, type);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode JWT payload", e);
        }
    }

    /**
     * Sign a JWT with HMAC-SHA256 using the given secret key bytes.
     */
    public String sign(Map<String, Object> payload, JwtHeader header, byte[] secretBytes) {
        try {
            byte[] payloadBytes = JSON.writeValueAsBytes(payload);
            Key key = new SecretKeySpec(secretBytes, "HmacSHA256");
            return JwtProcessor.sign(header, payloadBytes, key);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign JWT", e);
        }
    }
}
