package net.unit8.bouncr.data;

import java.net.URL;
import java.util.List;

/**
 * OIDC client application registered in Bouncr.
 *
 * @param id persistent identifier
 * @param name display name
 * @param nameLower normalized lowercase name for lookup
 * @param clientId OAuth2 client identifier
 * @param clientSecret OAuth2 client secret
 * @param privateKey private key for JWT signing when applicable
 * @param publicKey public key paired with {@code privateKey}
 * @param homeUrl home page URL
 * @param callbackUrl redirect/callback URL
 * @param description optional description
 * @param permissions permissions granted to this client
 */
public record OidcApplication(
    Long id,
    String name,
    String nameLower,
    String clientId,
    String clientSecret,
    byte[] privateKey,
    byte[] publicKey,
    URL homeUrl,
    URL callbackUrl,
    String description,
    List<Permission> permissions
) {}
