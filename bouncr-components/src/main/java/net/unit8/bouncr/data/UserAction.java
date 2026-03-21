package net.unit8.bouncr.data;

import java.time.LocalDateTime;

/**
 * Immutable audit log entry for user-related events.
 *
 * @param id persistent identifier
 * @param actionType action category
 * @param actor actor account identifier
 * @param actorIp source IP address
 * @param options free-form options/context payload
 * @param createdAt event timestamp
 */
public record UserAction(
    Long id,
    ActionType actionType,
    String actor,
    String actorIp,
    String options,
    LocalDateTime createdAt
) {}
