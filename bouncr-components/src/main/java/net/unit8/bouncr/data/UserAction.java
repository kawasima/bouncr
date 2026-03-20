package net.unit8.bouncr.data;

import java.time.LocalDateTime;

public record UserAction(
    Long id,
    ActionType actionType,
    String actor,
    String actorIp,
    String options,
    LocalDateTime createdAt
) {}
