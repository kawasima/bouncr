package net.unit8.bouncr.data;

/**
 * OAuth2 token introspection request (RFC 7662).
 *
 * @param token the token to introspect
 */
public record IntrospectionRequest(String token) {}
