package net.unit8.bouncr.data;

import java.io.Serializable;

/**
 * Data stored in the AUTHORIZATION_CODE store when Bouncr acts as an OIDC IdP.
 */
public record AuthorizationCode(
    String clientId,
    long userId,
    String userAccount,
    String scope,
    String nonce,
    String codeChallenge,
    String redirectUri,
    long createdAt
) implements Serializable {}
