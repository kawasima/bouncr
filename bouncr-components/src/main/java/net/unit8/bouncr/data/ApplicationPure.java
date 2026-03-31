package net.unit8.bouncr.data;

public record ApplicationPure(Long id, ApplicationSpec applicationSpec, boolean writeProtected) implements Application {
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
