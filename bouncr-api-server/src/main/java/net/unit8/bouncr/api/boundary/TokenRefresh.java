package net.unit8.bouncr.api.boundary;

/**
 * Request body for refreshing a session token.
 *
 * @param sessionId current session identifier to refresh
 */
public record TokenRefresh(String sessionId) {}
