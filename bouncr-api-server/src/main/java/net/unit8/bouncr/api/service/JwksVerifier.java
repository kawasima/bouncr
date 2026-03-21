package net.unit8.bouncr.api.service;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import net.unit8.bouncr.data.OidcProvider;

import java.io.InputStream;
import java.math.BigInteger;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Verifies ID token signatures using JWKS (JSON Web Key Set).
 * Caches JWKS per provider for 1 hour to avoid repeated HTTP calls.
 */
public class JwksVerifier {
    private static final long CACHE_TTL_MS = TimeUnit.HOURS.toMillis(1);
    private static final TypeReference<HashMap<String, Object>> JSON_REF = new TypeReference<>() {};

    private static final class CachedJwks {
        final List<Map<String, Object>> keys;
        final long fetchedAt;

        CachedJwks(List<Map<String, Object>> keys) {
            this.keys = keys;
            this.fetchedAt = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - fetchedAt > CACHE_TTL_MS;
        }
    }

    private final ConcurrentHashMap<Long, CachedJwks> cache = new ConcurrentHashMap<>();
    private final HttpClient httpClient;
    private final JsonMapper objectMapper = JsonMapper.builder().build();

    public JwksVerifier(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Verifies the signature of an encoded JWT (header.payload.signature) using the provider's JWKS.
     *
     * @return true if the signature is valid, false otherwise
     */
    public boolean verify(String encodedJwt, OidcProvider provider) {
        if (provider.jwksUri() == null) {
            // No JWKS URI configured — cannot verify signature (OpenID Connect Core §3.1.3.3)
            return false;
        }

        String[] parts = encodedJwt.split("\\.", 3);
        if (parts.length != 3) {
            return false;
        }

        try {
            Map<String, Object> header = objectMapper.readValue(
                    Base64.getUrlDecoder().decode(parts[0]), JSON_REF);
            String alg = (String) header.get("alg");
            String kid = (String) header.get("kid");

            List<Map<String, Object>> keys = getJwks(provider);
            PublicKey publicKey = findPublicKey(keys, kid, alg);
            if (publicKey == null) {
                return false;
            }

            String jcaAlgorithm = toJcaAlgorithm(alg);
            if (jcaAlgorithm == null) {
                return false;
            }

            byte[] signingInput = (parts[0] + "." + parts[1]).getBytes("UTF-8");
            byte[] signatureBytes = Base64.getUrlDecoder().decode(parts[2]);

            Signature sig = Signature.getInstance(jcaAlgorithm);
            sig.initVerify(publicKey);
            sig.update(signingInput);
            return sig.verify(signatureBytes);
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getJwks(OidcProvider provider) throws Exception {
        CachedJwks cached = cache.get(provider.id());
        if (cached != null && !cached.isExpired()) {
            return cached.keys;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(provider.jwksUri().toURI())
                .GET()
                .build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Failed to fetch JWKS: " + response.statusCode());
        }

        try (InputStream in = response.body()) {
            Map<String, Object> jwks = objectMapper.readValue(in, JSON_REF);
            List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");
            cache.put(provider.id(), new CachedJwks(keys));
            return keys;
        }
    }

    private PublicKey findPublicKey(List<Map<String, Object>> keys, String kid, String alg) throws Exception {
        for (Map<String, Object> key : keys) {
            String keyKid = (String) key.get("kid");
            if (kid != null && !kid.equals(keyKid)) {
                continue;
            }
            if ("RSA".equals(key.get("kty"))) {
                return buildRsaPublicKey(key);
            }
        }
        // Fallback to first RSA key only when JWT header has no kid (single-key providers)
        if (kid == null) {
            for (Map<String, Object> key : keys) {
                if ("RSA".equals(key.get("kty"))) {
                    return buildRsaPublicKey(key);
                }
            }
        }
        return null;
    }

    private PublicKey buildRsaPublicKey(Map<String, Object> key) throws Exception {
        byte[] nBytes = Base64.getUrlDecoder().decode((String) key.get("n"));
        byte[] eBytes = Base64.getUrlDecoder().decode((String) key.get("e"));
        BigInteger modulus = new BigInteger(1, nBytes);
        BigInteger exponent = new BigInteger(1, eBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(new RSAPublicKeySpec(modulus, exponent));
    }

    private String toJcaAlgorithm(String jwtAlg) {
        if (jwtAlg == null) return null;
        switch (jwtAlg) {
            case "RS256": return "SHA256withRSA";
            case "RS384": return "SHA384withRSA";
            case "RS512": return "SHA512withRSA";
            case "PS256": return "SHA256withRSAandMGF1";
            case "PS384": return "SHA384withRSAandMGF1";
            case "PS512": return "SHA512withRSAandMGF1";
            default: return null;
        }
    }
}
