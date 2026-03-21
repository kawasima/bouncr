package net.unit8.bouncr.api.decoder;

import net.unit8.bouncr.data.*;
import net.unit8.raoh.Decoder;
import net.unit8.raoh.Result;

import java.util.Map;
import java.util.Optional;

import static net.unit8.raoh.ObjectDecoders.*;
import static net.unit8.raoh.map.MapDecoders.*;

/**
 * Raoh-based decoders for {@code application/x-www-form-urlencoded} OAuth2 requests.
 *
 * <p>Uses {@link net.unit8.raoh.map.MapDecoders} to decode form parameters from
 * {@link enkan.collection.Parameters} (which implements {@code Map<String, Object>}).
 * This gives the same applicative validation and error accumulation as
 * {@link BouncrJsonDecoders} does for JSON request bodies.
 *
 * <h2>Usage in a Resource</h2>
 * <pre>{@code
 * @Decision(value = MALFORMED, method = "POST")
 * public Problem isMalformed(Parameters params, RestContext context) {
 *     return switch (BouncrFormDecoders.TOKEN_REQUEST.decode(params)) {
 *         case Ok<TokenRequest> ok -> { context.put(TOKEN_REQ, ok.value()); yield null; }
 *         case Err<TokenRequest> err -> {
 *             yield Problem.fromViolationList(err.issues().asList().stream()
 *                 .map(i -> new Problem.Violation(i.path().toString(), i.code(), i.message()))
 *                 .toList());
 *         }
 *     };
 * }
 * }</pre>
 *
 * @see BouncrJsonDecoders
 * @see net.unit8.raoh.map.MapDecoders
 */
public final class BouncrFormDecoders {

    private BouncrFormDecoders() {}

    // ==================== OAuth2 Authorize Request ====================

    /**
     * Decoded authorization request parameters.
     *
     * @param responseType      must be "code"
     * @param clientId          the requesting client's ID
     * @param redirectUri       must match registered callback URL
     * @param scope             requested scopes (parsed into {@link Scope})
     * @param state             opaque state for CSRF protection
     * @param nonce             for ID token replay protection
     * @param pkce              PKCE challenge (null if not using PKCE)
     */
    public record AuthorizeRequest(
            String responseType,
            String clientId,
            String redirectUri,
            Scope scope,
            String state,
            String nonce,
            PkceChallenge pkce
    ) {}

    /**
     * Decoder for {@code GET /oauth2/authorize} query parameters.
     */
    public static final Decoder<Map<String, Object>, AuthorizeRequest> AUTHORIZE_REQUEST = combine(
            field("response_type", string().nonBlank()),
            field("client_id", string().nonBlank()),
            field("redirect_uri", string().nonBlank()),
            field("scope", string().nonBlank()),
            optionalField("state", string()),
            optionalField("nonce", string()),
            optionalField("code_challenge", string()),
            optionalField("code_challenge_method", string())
    ).map((rt, cid, ru, scope, state, nonce, cc, ccm) -> {
        PkceChallenge pkce = null;
        if (cc.isPresent()) {
            try {
                pkce = new PkceChallenge(cc.get(), ccm.orElse("S256"));
            } catch (IllegalArgumentException e) {
                // Invalid PKCE method — pkce stays null, handled by resource
            }
        }
        return new AuthorizeRequest(rt, cid, ru, Scope.parse(scope),
                state.orElse(null), nonce.orElse(null), pkce);
    });

    // ==================== OAuth2 Token Requests ====================

    /**
     * Sealed interface for token request grant types.
     * Each variant carries only the parameters relevant to that grant type.
     */
    public sealed interface TokenRequest {
        /**
         * Authorization Code Grant — exchanges an authorization code for tokens.
         *
         * @param code         the authorization code from the authorize endpoint
         * @param redirectUri  must match the original authorization request
         * @param codeVerifier PKCE proof (null if PKCE not used)
         */
        record AuthorizationCodeGrant(String code, String redirectUri, String codeVerifier)
                implements TokenRequest {}

        /**
         * Refresh Token Grant — exchanges a refresh token for new tokens.
         *
         * @param refreshToken the opaque refresh token
         * @param scope        requested scope restriction (null = use original)
         */
        record RefreshTokenGrant(String refreshToken, Scope scope) implements TokenRequest {}

        /**
         * Client Credentials Grant — server-to-server, no user involved.
         *
         * @param scope requested scopes
         */
        record ClientCredentialsGrant(Scope scope) implements TokenRequest {}
    }

    private static final Decoder<Map<String, Object>, TokenRequest> AUTH_CODE_GRANT = combine(
            field("code", string().nonBlank()),
            optionalField("redirect_uri", string()),
            optionalField("code_verifier", string())
    ).map((code, ru, cv) ->
            (TokenRequest) new TokenRequest.AuthorizationCodeGrant(
                    code, ru.orElse(null), cv.orElse(null)));

    private static final Decoder<Map<String, Object>, TokenRequest> REFRESH_GRANT = combine(
            field("refresh_token", string().nonBlank()),
            optionalField("scope", string().nonBlank())
    ).map((rt, scope) ->
            (TokenRequest) new TokenRequest.RefreshTokenGrant(
                    rt, scope.map(Scope::parse).orElse(null)));

    private static final Decoder<Map<String, Object>, TokenRequest> CLIENT_CREDS_GRANT =
            optionalField("scope", string().nonBlank())
                    .map(scope -> (TokenRequest) new TokenRequest.ClientCredentialsGrant(
                            scope.map(Scope::parse).orElse(Scope.parse("openid"))));

    /**
     * Decoder for {@code POST /oauth2/token} form parameters.
     *
     * <p>First extracts {@code grant_type} to determine the variant, then
     * delegates to the appropriate sub-decoder. Unknown grant types produce
     * a validation error.
     */
    public static final Decoder<Map<String, Object>, TokenRequest> TOKEN_REQUEST = (in, path) -> {
        if (in == null) {
            return Result.fail(path, "required", "request is empty");
        }
        Object grantTypeObj = in.get("grant_type");
        if (!(grantTypeObj instanceof String grantTypeStr) || grantTypeStr.isBlank()) {
            return Result.fail(path.append("grant_type"), "required", "grant_type is required");
        }
        Optional<GrantType> grantType = GrantType.fromString(grantTypeStr);
        if (grantType.isEmpty()) {
            return Result.fail(path.append("grant_type"), "unsupported",
                    "Unsupported grant_type: " + grantTypeStr);
        }
        return switch (grantType.get()) {
            case AUTHORIZATION_CODE -> AUTH_CODE_GRANT.decode(in, path);
            case REFRESH_TOKEN -> REFRESH_GRANT.decode(in, path);
            case CLIENT_CREDENTIALS -> CLIENT_CREDS_GRANT.decode(in, path);
        };
    };

    // ==================== Token Introspection ====================

    /**
     * Decoded introspection request.
     *
     * @param token the token to introspect
     */
    public record IntrospectionRequest(String token) {}

    /**
     * Decoder for {@code POST /oauth2/token/introspect} form parameters.
     */
    public static final Decoder<Map<String, Object>, IntrospectionRequest> INTROSPECTION_REQUEST =
            field("token", string().nonBlank())
                    .map(IntrospectionRequest::new);

    // ==================== Token Revocation ====================

    /**
     * Decoded revocation request.
     *
     * @param token         the token to revoke
     * @param tokenTypeHint optional hint about the token type
     */
    public record RevocationRequest(String token, String tokenTypeHint) {}

    /**
     * Decoder for {@code POST /oauth2/token/revoke} form parameters.
     */
    public static final Decoder<Map<String, Object>, RevocationRequest> REVOCATION_REQUEST = combine(
            field("token", string().nonBlank()),
            optionalField("token_type_hint", string())
    ).map((token, hint) -> new RevocationRequest(token, hint.orElse(null)));
}
