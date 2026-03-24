package net.unit8.bouncr.data;

/**
 * External OpenID Connect identity provider configuration.
 *
 * <p>All fields are non-null when loaded from the database. For contexts
 * where only the provider identity is needed (e.g., listing linked
 * providers for a user), use {@code providerName} directly.
 *
 * @param id               persistent identifier
 * @param name             display name
 * @param nameLower        normalized lowercase name for lookup
 * @param providerMetadata provider-published endpoints and identity (RFC 8414)
 * @param clientConfig     client registration settings for this provider
 */
public record OidcProvider(
    Long id,
    String name,
    String nameLower,
    OidcProviderMetadata providerMetadata,
    OidcProviderClientConfig clientConfig
) {}
