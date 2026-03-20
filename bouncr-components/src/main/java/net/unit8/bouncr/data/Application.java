package net.unit8.bouncr.data;

import java.util.List;

public record Application(Long id, String name, String description, String passTo, String virtualPath, String topPage, Boolean writeProtected, List<Realm> realms) {
}
