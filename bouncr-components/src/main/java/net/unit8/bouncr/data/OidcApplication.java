package net.unit8.bouncr.data;

import java.net.URL;
import java.util.List;

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
