package net.unit8.bouncr.hook.license.entity;

import java.time.LocalDateTime;

public record LicenseLastActivity(Long id, Long userLicenseId, String userAgent, LocalDateTime lastUsedAt) {
}
