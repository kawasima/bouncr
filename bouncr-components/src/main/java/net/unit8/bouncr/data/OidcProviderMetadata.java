package net.unit8.bouncr.data;

import java.net.URI;

/**
 * OpenID Provider Metadata (RFC 8414) — endpoints and identity published by the provider.
 *
 * @param authorizationEndpoint provider's authorization endpoint URL
 * @param tokenEndpoint         provider's token endpoint URL
 * @param jwksUri               provider's JWKS endpoint URI
 * @param issuer                expected issuer identifier
 */
public record OidcProviderMetadata(
    String authorizationEndpoint,
    String tokenEndpoint,
    URI jwksUri,
    String issuer
) {}
