package net.unit8.bouncr.data;

/**
 * External OpenID Connect identity provider configuration.
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
) {
    /** Factory for summary use — creates an OidcProvider with only identity fields. */
    public static OidcProvider ofSummary(Long id, String name, String nameLower) {
        return new OidcProvider(id, name, nameLower, null, null);
    }
}
