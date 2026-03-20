package net.unit8.bouncr.data;

import java.util.List;

public record Permission(
    Long id,
    String name,
    String description,
    Boolean writeProtected,
    List<Role> roles
) {
}