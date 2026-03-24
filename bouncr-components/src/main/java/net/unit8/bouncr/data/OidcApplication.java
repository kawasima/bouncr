package net.unit8.bouncr.data;

import java.util.List;

/**
 * OIDC client application registered in Bouncr.
 *
 * @param id          persistent identifier
 * @param name        display name
 * @param nameLower   normalized lowercase name for lookup
 * @param credentials OAuth2 client credentials (client_id + client_secret)
 * @param signingKeys JWT signing key pair (nullable if not applicable)
 * @param metadata    OIDC client registration metadata (URIs + grant types)
 * @param description optional description
 * @param permissions permissions granted to this client
 */
public record OidcApplication(
    Long id,
    String name,
    String nameLower,
    ClientCredentials credentials,
    SigningKeyPair signingKeys,
    OidcClientMetadata metadata,
    String description,
    List<Permission> permissions
) {}
