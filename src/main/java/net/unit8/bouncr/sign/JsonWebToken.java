package net.unit8.bouncr.sign;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import enkan.collection.OptionMap;
import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import enkan.exception.MisconfigurationException;
import net.unit8.bouncr.component.BouncrConfiguration;

import java.security.*;
import java.util.Base64;
import java.util.Map;

import static enkan.util.ThreadingUtils.some;

public class JsonWebToken extends SystemComponent {
    private ObjectMapper mapper;
    private Base64.Decoder base64Decoder;
    private Base64.Encoder base64Encoder;

    private static final OptionMap ALGORITHMS = OptionMap.of(
            "HS256", "HmacSHA256",
            "HS384", "HmacSHA384",
            "HS512", "HmacSHA512",
            "RS256", "SHA256withRSA",
            "RS384", "SHA384withRSA",
            "RS512", "SHA512withRSA",
            "PS256", "SHA256withRSAandMGF1",
            "PS384", "SHA384withRSAandMGF1",
            "PS512", "SHA512withRSAandMGF1",
            "none",  "none"
            );

    private String encodeHeader(JwtHeader header) {
        return some(header,
                h -> mapper.writeValueAsBytes(h),
                json -> base64Encoder.encodeToString(json))
                .orElse(null);
    }

    public JwtClaim decodePayload(String encoded) {
        return some(encoded,
                enc -> new String(base64Decoder.decode(enc)),
                plain -> mapper.readValue(plain, JwtClaim.class))
                .orElse(null);
    }


    public String sign(String payload, JwtHeader header, PrivateKey key) {
        String encodedHeader = encodeHeader(header);
        try {
            String signAlgorithm = ALGORITHMS.getString(header.getAlg());
            if (signAlgorithm == null) throw new MisconfigurationException("bouncr.NO_SUCH_JWT_ALGORITHM", header.getAlg());
            String encodedSignature = "";
            if (!signAlgorithm.equals("none")) {
                Signature signature = Signature.getInstance(signAlgorithm, "BC");
                SecureRandom prng = getDependency(BouncrConfiguration.class).getSecureRandom();
                signature.initSign(key, prng);
                signature.update(String.join(".", encodedHeader, payload).getBytes());
                encodedSignature = Base64.getEncoder().encodeToString(signature.sign());
            }
            return String.join(".", encodedHeader, payload, encodedSignature);
        } catch (NoSuchAlgorithmException e) {
            throw new MisconfigurationException(""); //TODO
        } catch (SignatureException e) {
            throw new MisconfigurationException(""); //TODO
        } catch (NoSuchProviderException e) {
            throw new MisconfigurationException(""); //TODO
        } catch (InvalidKeyException e) {
            throw new MisconfigurationException(""); //TODO
        }
    }

    public String sign(Map<String, Object> claims, JwtHeader header, PrivateKey key) {
        String encodedPayload = some(claims,
                p -> mapper.writeValueAsBytes(p),
                s -> Base64.getEncoder().encodeToString(s)).orElse(null);
        return sign(encodedPayload, header, key);
    }

    public String sign(JwtClaim claims, JwtHeader header, PrivateKey key) {
        String encodedPayload = some(claims,
                p -> mapper.writeValueAsBytes(p),
                s -> Base64.getEncoder().encodeToString(s)).orElse(null);
        return sign(encodedPayload, header, key);
    }

    @Override
    protected ComponentLifecycle lifecycle() {
        return new ComponentLifecycle<JsonWebToken>() {
            @Override
            public void start(JsonWebToken component) {
                component.mapper = new ObjectMapper();
                component.mapper.registerModule(new JavaTimeModule());
                component.mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
                component.mapper.configure(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS, true);
                component.mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
                component.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

                component.base64Decoder = Base64.getUrlDecoder();
                component.base64Encoder = Base64.getUrlEncoder();
            }

            @Override
            public void stop(JsonWebToken component) {
                component.mapper = null;
            }
        };
    }
}
