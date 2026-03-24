package net.unit8.bouncr.data;

/**
 * OAuth2 token revocation request (RFC 7009).
 *
 * @param token         the token to revoke
 * @param tokenTypeHint optional hint about the token type
 */
public record RevocationRequest(String token, String tokenTypeHint) {}
