package net.unit8.bouncr.data;

/**
 * Sealed interface for OAuth2 token request grant types.
 * Each variant carries only the parameters relevant to that grant type.
 */
public sealed interface TokenRequest {
    record AuthorizationCodeGrant(String code, String redirectUri, String codeVerifier)
            implements TokenRequest {}

    record RefreshTokenGrant(String refreshToken, Scope scope) implements TokenRequest {}

    record ClientCredentialsGrant(Scope scope) implements TokenRequest {}
}
