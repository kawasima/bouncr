package net.unit8.bouncr.api.boundary;

/**
 * Request body for updating an OIDC provider.
 *
 * @param name display name of the OIDC provider
 * @param clientId OAuth 2.0 client identifier
 * @param clientSecret OAuth 2.0 client secret
 * @param scope space-delimited OAuth scopes to request
 * @param responseType OAuth response type (e.g. "code")
 * @param authorizationEndpoint URL of the authorization endpoint
 * @param tokenEndpoint URL of the token endpoint
 * @param tokenEndpointAuthMethod authentication method for the token endpoint
 * @param redirectUri redirect URI registered with the provider
 * @param jwksUri URL of the provider's JWKS endpoint
 * @param issuer expected issuer claim in ID tokens
 * @param pkceEnabled whether PKCE is enabled for this provider
 */
public record OidcProviderUpdate(String name, String clientId, String clientSecret, String scope,
                                  String responseType, String authorizationEndpoint, String tokenEndpoint,
                                  String tokenEndpointAuthMethod, String redirectUri, String jwksUri,
                                  String issuer, boolean pkceEnabled) {}
