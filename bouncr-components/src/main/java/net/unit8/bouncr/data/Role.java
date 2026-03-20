package net.unit8.bouncr.data;

import java.util.List;

public record Role(
    Long id,
    String name,
    String description,
    Boolean writeProtected,
    List<Permission> permissions
) {}