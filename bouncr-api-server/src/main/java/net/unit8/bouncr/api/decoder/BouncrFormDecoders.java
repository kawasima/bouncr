package net.unit8.bouncr.api.decoder;

import net.unit8.bouncr.data.*;
import net.unit8.raoh.decode.Decoder;

import java.util.Map;

import static net.unit8.raoh.decode.ObjectDecoders.*;
import static net.unit8.raoh.decode.map.MapDecoders.*;

/**
 * Raoh-based decoders for {@code application/x-www-form-urlencoded} OAuth2 requests.
 *
 * <p>Uses {@link net.unit8.raoh.decode.map.MapDecoders} to decode form parameters from
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
 * @see net.unit8.raoh.decode.map.MapDecoders
 */
public final class BouncrFormDecoders {

    private BouncrFormDecoders() {}

    // ==================== OAuth2 Authorize Request ====================

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
     * <p>Dispatches on the {@code grant_type} field to the appropriate
     * sub-decoder using {@link net.unit8.raoh.decode.map.MapDecoders#discriminate}.
     * Unknown grant types produce a validation error automatically.
     */
    public static final Decoder<Map<String, Object>, TokenRequest> TOKEN_REQUEST = discriminate(
            "grant_type", Map.of(
                    "authorization_code", AUTH_CODE_GRANT,
                    "refresh_token",      REFRESH_GRANT,
                    "client_credentials", CLIENT_CREDS_GRANT
            ));

    // ==================== Token Introspection ====================

    /**
     * Decoder for {@code POST /oauth2/token/introspect} form parameters.
     */
    public static final Decoder<Map<String, Object>, IntrospectionRequest> INTROSPECTION_REQUEST =
            field("token", string().nonBlank())
                    .map(IntrospectionRequest::new);

    // ==================== Token Revocation ====================

    /**
     * Decoder for {@code POST /oauth2/token/revoke} form parameters.
     */
    public static final Decoder<Map<String, Object>, RevocationRequest> REVOCATION_REQUEST = combine(
            field("token", string().nonBlank()),
            optionalField("token_type_hint", string())
    ).map((token, hint) -> new RevocationRequest(token, hint.orElse(null)));
}
