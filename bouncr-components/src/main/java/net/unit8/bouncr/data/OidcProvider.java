package net.unit8.bouncr.data;

import java.net.URI;

/**
 * External OpenID Connect identity provider configuration.
 *
 * @param id persistent identifier
 * @param name display name
 * @param nameLower normalized lowercase name for lookup
 * @param clientId client ID used against the provider
 * @param clientSecret client secret used against the provider
 * @param scope requested upstream scope string
 * @param responseType response type used for upstream authorization requests
 * @param tokenEndpoint provider token endpoint URL
 * @param authorizationEndpoint provider authorization endpoint URL
 * @param tokenEndpointAuthMethod client authentication method for token endpoint
 * @param redirectUri callback URI registered at the provider
 * @param jwksUri provider JWKS endpoint
 * @param issuer expected issuer identifier
 * @param pkceEnabled whether PKCE is enabled for this provider
 */
public record OidcProvider(
    Long id,
    String name,
    String nameLower,
    String clientId,
    String clientSecret,
    String scope,
    ResponseType responseType,
    String tokenEndpoint,
    String authorizationEndpoint,
    TokenEndpointAuthMethod tokenEndpointAuthMethod,
    URI redirectUri,
    URI jwksUri,
    String issuer,
    boolean pkceEnabled
) {
    /** Factory for summary use — creates an OidcProvider with only identity fields. */
    public static OidcProvider ofSummary(Long id, String name, String nameLower) {
        return new OidcProvider(id, name, nameLower,
                null, null, null, null, null, null, null, null, null, null, false);
    }
}
