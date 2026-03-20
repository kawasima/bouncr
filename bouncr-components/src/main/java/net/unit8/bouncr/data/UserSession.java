package net.unit8.bouncr.data;

import java.time.LocalDateTime;

public record UserSession(
        Long id,
        User user,
        String token,
        String remoteAddress,
        String userAgent,
        LocalDateTime createdAt
) {
}
