package net.unit8.bouncr.data;

import net.unit8.bouncr.entity.ResponseType;
import net.unit8.bouncr.util.RandomUtils;

import java.io.Serializable;
import java.security.SecureRandom;

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

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public ResponseType getResponseType() {
        return responseType;
    }

    public void setResponseType(ResponseType responseType) {
        this.responseType = responseType;
    }
}
