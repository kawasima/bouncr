package net.unit8.bouncr.data;

import java.io.Serializable;

public record OidcSession(
    String nonce,
    String state,
    ResponseType responseType,
    String codeVerifier
) implements Serializable {
}
