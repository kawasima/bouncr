package net.unit8.bouncr.data;

import java.net.URI;

/**
 * Client registration configuration for an external OIDC provider.
 *
 * @param credentials             client credentials (client_id + client_secret)
 * @param scope                   requested upstream scope string
 * @param responseType            response type for authorization requests
 * @param tokenEndpointAuthMethod client authentication method at the token endpoint
 * @param redirectUri             callback URI registered at the provider
 * @param pkceEnabled             whether PKCE (S256) is enabled
 */
public record OidcProviderClientConfig(
    ClientCredentials credentials,
    String scope,
    ResponseType responseType,
    TokenEndpointAuthMethod tokenEndpointAuthMethod,
    URI redirectUri,
    boolean pkceEnabled
) {}
