package net.unit8.bouncr.sign;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import enkan.exception.MisconfigurationException;

import java.security.*;
import java.util.Base64;

import static enkan.util.ThreadingUtils.some;

public class IdToken extends SystemComponent {
    private ObjectMapper mapper;
    private Base64.Decoder base64Decoder;
    private Base64.Encoder base64Encoder;

    private String encodeHeader(IdTokenHeader header) {
        return some(header,
                h -> mapper.writeValueAsBytes(h),
                json -> base64Encoder.encodeToString(json))
                .orElse(null);
    }

    public IdTokenPayload decodePayload(String encoded) {
        return some(encoded,
                enc -> new String(base64Decoder.decode(enc)),
                plain -> mapper.readValue(plain, IdTokenPayload.class))
                .orElse(null);
    }

    public String sign(IdTokenPayload payload, IdTokenHeader header, PrivateKey key) {
        String encodedHeader = encodeHeader(header);
        String encodedPayload = some(payload,
                p -> mapper.writeValueAsBytes(p),
                s -> Base64.getEncoder().encodeToString(s)).orElse(null);

        try {
            Signature signature = Signature.getInstance("SHA256withRSAandMGF1", "BC");
            SecureRandom prng = SecureRandom.getInstance("NativePRNGNonBlocking");
            signature.initSign(key, prng);
            signature.update(String.join(".", encodedHeader, encodedPayload).getBytes());
            String encodedSignature = Base64.getEncoder().encodeToString(signature.sign());
            return String.join(".", encodedHeader, encodedPayload, encodedSignature);
        } catch (NoSuchAlgorithmException e) {
            throw new MisconfigurationException("");
        } catch (SignatureException e) {
            throw new MisconfigurationException("");
        } catch (NoSuchProviderException e) {
            throw new MisconfigurationException("");
        } catch (InvalidKeyException e) {
            throw new MisconfigurationException("");
        }
    }

    @Override
    protected ComponentLifecycle lifecycle() {
        return new ComponentLifecycle<IdToken>() {
            @Override
            public void start(IdToken component) {
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
            public void stop(IdToken component) {
                component.mapper = null;
            }
        };
    }
}
