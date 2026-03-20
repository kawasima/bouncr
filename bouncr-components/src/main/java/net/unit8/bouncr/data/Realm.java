package net.unit8.bouncr.data;

import java.util.List;

public record Realm(
    Long id,
    String name,
    String nameLower,
    String url,
    String description,
    Application application,
    Boolean writeProtected,
    List<Assignment> assignments
) {
}