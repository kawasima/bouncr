package net.unit8.bouncr.api.service;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class SignatureVerifierTest {

    private static final String KEY = "test-secret-key";
    private static final String PAYLOAD = "test-session-id";

    private String buildHeader(String key, long timestamp, String payload) throws Exception {
        String signed = timestamp + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String sig = HexFormat.of().formatHex(mac.doFinal(signed.getBytes(StandardCharsets.UTF_8)));
        return "t=" + timestamp + ",v1=" + sig;
    }

    @Test
    void validSignature() throws Exception {
        long now = System.currentTimeMillis() / 1000;
        String header = buildHeader(KEY, now, PAYLOAD);
        assertThat(new SignatureVerifier(KEY).verify(header, PAYLOAD)).isTrue();
    }

    @Test
    void wrongKey() throws Exception {
        long now = System.currentTimeMillis() / 1000;
        String header = buildHeader("wrong-key", now, PAYLOAD);
        assertThat(new SignatureVerifier(KEY).verify(header, PAYLOAD)).isFalse();
    }

    @Test
    void wrongPayload() throws Exception {
        long now = System.currentTimeMillis() / 1000;
        String header = buildHeader(KEY, now, PAYLOAD);
        assertThat(new SignatureVerifier(KEY).verify(header, "different-payload")).isFalse();
    }

    @Test
    void nullHeader() {
        assertThat(new SignatureVerifier(KEY).verify(null, PAYLOAD)).isFalse();
    }

    @Test
    void malformedHeader() {
        assertThat(new SignatureVerifier(KEY).verify("garbage", PAYLOAD)).isFalse();
    }

    @Test
    void expiredTimestamp() throws Exception {
        long oldTimestamp = 1_000_000_000L; // far in the past
        String header = buildHeader(KEY, oldTimestamp, PAYLOAD);
        assertThat(new SignatureVerifier(KEY).verify(header, PAYLOAD)).isFalse();
    }

    @Test
    void invalidHexSignature() {
        long now = System.currentTimeMillis() / 1000;
        String header = "t=" + now + ",v1=not-valid-hex!!";
        assertThat(new SignatureVerifier(KEY).verify(header, PAYLOAD)).isFalse();
    }

    @Test
    void missingTimestampPart() {
        assertThat(new SignatureVerifier(KEY).verify("v1=deadbeef", PAYLOAD)).isFalse();
    }

    @Test
    void missingSignaturePart() throws Exception {
        long now = System.currentTimeMillis() / 1000;
        assertThat(new SignatureVerifier(KEY).verify("t=" + now, PAYLOAD)).isFalse();
    }
}
