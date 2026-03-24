package net.unit8.bouncr.api.decoder;

import net.unit8.bouncr.data.AuthorizeRequest;
import net.unit8.bouncr.data.IntrospectionRequest;
import net.unit8.bouncr.data.RevocationRequest;
import net.unit8.bouncr.data.TokenRequest;
import net.unit8.bouncr.data.TokenRequest.AuthorizationCodeGrant;
import net.unit8.bouncr.data.TokenRequest.ClientCredentialsGrant;
import net.unit8.bouncr.data.TokenRequest.RefreshTokenGrant;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for BouncrFormDecoders — Raoh MapDecoders applied to OAuth2 form parameters.
 */
class BouncrFormDecodersTest {

    // ==================== AuthorizeRequest ====================

    @Test
    void authorizeRequest_validParams_decodesSuccessfully() {
        Map<String, Object> params = Map.of(
                "response_type", "code",
                "client_id", "my-client",
                "redirect_uri", "https://example.com/callback",
                "scope", "openid profile email",
                "state", "xyz123",
                "nonce", "nonce456");

        var result = BouncrFormDecoders.AUTHORIZE_REQUEST.decode(params);
        assertThat(result).isInstanceOf(Ok.class);

        AuthorizeRequest req = ((Ok<AuthorizeRequest>) result).value();
        assertThat(req.responseType()).isEqualTo("code");
        assertThat(req.clientId()).isEqualTo("my-client");
        assertThat(req.redirectUri()).isEqualTo("https://example.com/callback");
        assertThat(req.scope().contains("openid")).isTrue();
        assertThat(req.scope().contains("profile")).isTrue();
        assertThat(req.scope().contains("email")).isTrue();
        assertThat(req.state()).isEqualTo("xyz123");
        assertThat(req.nonce()).isEqualTo("nonce456");
        assertThat(req.pkce()).isNull();
    }

    @Test
    void authorizeRequest_withPkce_createsPkceChallenge() {
        Map<String, Object> params = Map.of(
                "response_type", "code",
                "client_id", "my-client",
                "redirect_uri", "https://example.com/callback",
                "scope", "openid",
                "code_challenge", "abc123challenge",
                "code_challenge_method", "S256");

        var result = BouncrFormDecoders.AUTHORIZE_REQUEST.decode(params);
        assertThat(result).isInstanceOf(Ok.class);

        AuthorizeRequest req = ((Ok<AuthorizeRequest>) result).value();
        assertThat(req.pkce()).isNotNull();
        assertThat(req.pkce().challenge()).isEqualTo("abc123challenge");
        assertThat(req.pkce().method()).isEqualTo("S256");
    }

    @Test
    void authorizeRequest_missingRequiredFields_returnsErrors() {
        Map<String, Object> params = Map.of();

        var result = BouncrFormDecoders.AUTHORIZE_REQUEST.decode(params);
        assertThat(result).isInstanceOf(Err.class);
    }

    // ==================== TokenRequest — Authorization Code ====================

    @Test
    void tokenRequest_authorizationCode_decodesSuccessfully() {
        Map<String, Object> params = new HashMap<>();
        params.put("grant_type", "authorization_code");
        params.put("code", "auth-code-123");
        params.put("redirect_uri", "https://example.com/callback");
        params.put("code_verifier", "verifier-abc");

        var result = BouncrFormDecoders.TOKEN_REQUEST.decode(params);
        assertThat(result).isInstanceOf(Ok.class);

        TokenRequest req = ((Ok<TokenRequest>) result).value();
        assertThat(req).isInstanceOf(AuthorizationCodeGrant.class);

        AuthorizationCodeGrant grant = (AuthorizationCodeGrant) req;
        assertThat(grant.code()).isEqualTo("auth-code-123");
        assertThat(grant.redirectUri()).isEqualTo("https://example.com/callback");
        assertThat(grant.codeVerifier()).isEqualTo("verifier-abc");
    }

    // ==================== TokenRequest — Refresh Token ====================

    @Test
    void tokenRequest_refreshToken_decodesSuccessfully() {
        Map<String, Object> params = Map.of(
                "grant_type", "refresh_token",
                "refresh_token", "rt-uuid-123");

        var result = BouncrFormDecoders.TOKEN_REQUEST.decode(params);
        assertThat(result).isInstanceOf(Ok.class);

        TokenRequest req = ((Ok<TokenRequest>) result).value();
        assertThat(req).isInstanceOf(RefreshTokenGrant.class);

        RefreshTokenGrant grant = (RefreshTokenGrant) req;
        assertThat(grant.refreshToken()).isEqualTo("rt-uuid-123");
        assertThat(grant.scope()).isNull(); // not specified → use original
    }

    @Test
    void tokenRequest_refreshToken_withScopeRestriction() {
        Map<String, Object> params = Map.of(
                "grant_type", "refresh_token",
                "refresh_token", "rt-uuid-123",
                "scope", "openid");

        var result = BouncrFormDecoders.TOKEN_REQUEST.decode(params);
        assertThat(result).isInstanceOf(Ok.class);

        RefreshTokenGrant grant = (RefreshTokenGrant) ((Ok<TokenRequest>) result).value();
        assertThat(grant.scope()).isNotNull();
        assertThat(grant.scope().contains("openid")).isTrue();
    }

    // ==================== TokenRequest — Client Credentials ====================

    @Test
    void tokenRequest_clientCredentials_decodesSuccessfully() {
        Map<String, Object> params = Map.of(
                "grant_type", "client_credentials",
                "scope", "openid profile");

        var result = BouncrFormDecoders.TOKEN_REQUEST.decode(params);
        assertThat(result).isInstanceOf(Ok.class);

        TokenRequest req = ((Ok<TokenRequest>) result).value();
        assertThat(req).isInstanceOf(ClientCredentialsGrant.class);

        ClientCredentialsGrant grant = (ClientCredentialsGrant) req;
        assertThat(grant.scope().contains("openid")).isTrue();
        assertThat(grant.scope().contains("profile")).isTrue();
    }

    @Test
    void tokenRequest_clientCredentials_defaultsToOpenid() {
        Map<String, Object> params = Map.of("grant_type", "client_credentials");

        var result = BouncrFormDecoders.TOKEN_REQUEST.decode(params);
        assertThat(result).isInstanceOf(Ok.class);

        ClientCredentialsGrant grant = (ClientCredentialsGrant) ((Ok<TokenRequest>) result).value();
        assertThat(grant.scope().contains("openid")).isTrue();
    }

    // ==================== TokenRequest — Error Cases ====================

    @Test
    void tokenRequest_missingGrantType_returnsError() {
        Map<String, Object> params = Map.of("code", "some-code");

        var result = BouncrFormDecoders.TOKEN_REQUEST.decode(params);
        assertThat(result).isInstanceOf(Err.class);
    }

    @Test
    void tokenRequest_unsupportedGrantType_returnsError() {
        Map<String, Object> params = Map.of("grant_type", "implicit");

        var result = BouncrFormDecoders.TOKEN_REQUEST.decode(params);
        assertThat(result).isInstanceOf(Err.class);
    }

    @Test
    void tokenRequest_authCodeWithoutCode_returnsError() {
        Map<String, Object> params = Map.of("grant_type", "authorization_code");

        var result = BouncrFormDecoders.TOKEN_REQUEST.decode(params);
        assertThat(result).isInstanceOf(Err.class);
    }

    // ==================== Introspection ====================

    @Test
    void introspectionRequest_validToken_decodes() {
        Map<String, Object> params = Map.of("token", "eyJhbGciOiJSUzI1NiJ9.payload.sig");

        var result = BouncrFormDecoders.INTROSPECTION_REQUEST.decode(params);
        assertThat(result).isInstanceOf(Ok.class);

        IntrospectionRequest req = ((Ok<IntrospectionRequest>) result).value();
        assertThat(req.token()).isEqualTo("eyJhbGciOiJSUzI1NiJ9.payload.sig");
    }

    @Test
    void introspectionRequest_missingToken_returnsError() {
        Map<String, Object> params = Map.of();

        var result = BouncrFormDecoders.INTROSPECTION_REQUEST.decode(params);
        assertThat(result).isInstanceOf(Err.class);
    }

    // ==================== Revocation ====================

    @Test
    void revocationRequest_validToken_decodes() {
        Map<String, Object> params = Map.of("token", "refresh-token-uuid");

        var result = BouncrFormDecoders.REVOCATION_REQUEST.decode(params);
        assertThat(result).isInstanceOf(Ok.class);

        RevocationRequest req = ((Ok<RevocationRequest>) result).value();
        assertThat(req.token()).isEqualTo("refresh-token-uuid");
        assertThat(req.tokenTypeHint()).isNull();
    }

    @Test
    void revocationRequest_withHint_decodes() {
        Map<String, Object> params = Map.of(
                "token", "refresh-token-uuid",
                "token_type_hint", "refresh_token");

        var result = BouncrFormDecoders.REVOCATION_REQUEST.decode(params);
        assertThat(result).isInstanceOf(Ok.class);

        RevocationRequest req = ((Ok<RevocationRequest>) result).value();
        assertThat(req.tokenTypeHint()).isEqualTo("refresh_token");
    }
}
