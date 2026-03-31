package net.unit8.bouncr.data;

import java.util.List;

public record ApplicationWithRealms(Long id, ApplicationSpec applicationSpec, boolean writeProtected, List<Realm> realms) implements Application {
    public static Application of(Application app, List<Realm> realms) {
        return new ApplicationWithRealms(app.id(), new ApplicationSpec(app.name(), app.description(), app.passTo(), app.virtualPath(), app.topPage()), app.writeProtected(), realms);
    }

    public WordName name() {
        return applicationSpec.name();
    }

    public String description() {
        return applicationSpec.description();
    }

    public String passTo() {
        return applicationSpec.passTo();
    }

    public String virtualPath() {
        return applicationSpec.virtualPath();
    }

    public String topPage() {
        return applicationSpec.topPage();
    }
}
