package net.unit8.bouncr.data;

import java.time.LocalDateTime;

/**
 * Persisted authenticated user session.
 *
 * @param id persistent identifier
 * @param user session owner
 * @param token session token
 * @param remoteAddress client IP address
 * @param userAgent client user agent
 * @param createdAt creation timestamp
 */
public record UserSession(
        Long id,
        User user,
        String token,
        String remoteAddress,
        String userAgent,
        LocalDateTime createdAt
) {
}
