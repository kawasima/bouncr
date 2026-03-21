package net.unit8.bouncr.api.service;

import com.sun.net.httpserver.HttpServer;
import net.unit8.bouncr.data.OidcProvider;
import net.unit8.bouncr.data.ResponseType;
import net.unit8.bouncr.data.TokenEndpointAuthMethod;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class JwksVerifierTest {
    @Test
    void verifyWithRsaJwks() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        String kid = "test-kid";

        String jwks = """
                {"keys":[{"kty":"RSA","kid":"%s","n":"%s","e":"%s"}]}
                """.formatted(
                kid,
                base64Url(publicKey.getModulus().toByteArray()),
                base64Url(publicKey.getPublicExponent().toByteArray()));

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/jwks", exchange -> {
            byte[] body = jwks.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("content-type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();

        try {
            OidcProvider provider = new OidcProvider(
                    1L, "p", "p", "cid", "sec", "openid",
                    ResponseType.CODE, null, null, TokenEndpointAuthMethod.CLIENT_SECRET_POST,
                    null,
                    new java.net.URL("http://localhost:%d/jwks".formatted(server.getAddress().getPort())),
                    "issuer", false
            );
            JwksVerifier verifier = new JwksVerifier(HttpClient.newHttpClient());

            String token = createJwt("RS256", kid, keyPair, "{\"sub\":\"u1\"}");
            assertThat(verifier.verify(token, provider)).isTrue();

            String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "AAAA";
            assertThat(verifier.verify(tampered, provider)).isFalse();
        } finally {
            server.stop(0);
        }
    }

    private static String createJwt(String alg, String kid, KeyPair keyPair, String payloadJson) throws Exception {
        String headerJson = "{\"alg\":\"" + alg + "\",\"kid\":\"" + kid + "\",\"typ\":\"JWT\"}";
        String header = base64Url(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = base64Url(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signingInput = header + "." + payload;

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(signingInput.getBytes(StandardCharsets.UTF_8));
        String sign = base64Url(signature.sign());
        return signingInput + "." + sign;
    }

    private static String base64Url(byte[] bytes) {
        // JWK n/e must be unsigned big-endian integers
        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] trimmed = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, trimmed, 0, trimmed.length);
            bytes = trimmed;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
