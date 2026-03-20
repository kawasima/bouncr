package net.unit8.bouncr.data;

import java.net.URI;
import java.net.URL;

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
    URL jwksUri,
    String issuer,
    boolean pkceEnabled
) {}
