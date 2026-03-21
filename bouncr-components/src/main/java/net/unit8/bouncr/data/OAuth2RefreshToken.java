package net.unit8.bouncr.data;

import java.io.Serializable;

/**
 * Data stored in the ACCESS_TOKEN store for OAuth2 refresh tokens.
 * The key is the opaque refresh token UUID.
 */
public record OAuth2RefreshToken(
    String clientId,
    long userId,
    String userAccount,
    String scope,
    long createdAt
) implements Serializable {}
