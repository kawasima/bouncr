package net.unit8.bouncr.web.controller.data;

import lombok.Data;
import lombok.NoArgsConstructor;
import net.unit8.bouncr.util.RandomUtils;
import net.unit8.bouncr.web.entity.ResponseType;

import java.io.Serializable;
import java.security.SecureRandom;

@Data
@NoArgsConstructor
public class OidcSession implements Serializable {
    public static OidcSession create(SecureRandom prng) {
        OidcSession session = new OidcSession();
        session.setNonce(RandomUtils.generateRandomString(32, prng));
        session.setState(RandomUtils.generateRandomString(16, prng));
        return session;
    }

    private String nonce;
    private String state;
    private ResponseType responseType;
}
