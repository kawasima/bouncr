package net.unit8.bouncr.proxy;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author kawasima
 */
public class LocationConfig {
    private String path;
    private String virtualPath;
    private URI pass;

    protected LocationConfig() {

    }

    public String getPath() {
        return path;
    }

    public String getVirtualPath() {
        return virtualPath;
    }
    public URI getPass() {
        return pass;
    }

    public static Builder builder() {
        return new Builder(new LocationConfig());
    }

    public static class Builder {
        private LocationConfig cfg;

        Builder(LocationConfig cfg) {
            this.cfg = cfg;
        }

        public Builder setPath(String path) {
            cfg.path = path;
            return this;
        }

        public Builder setVirtualPath(String virtualPath) {
            cfg.virtualPath = virtualPath;
            return this;
        }

        public Builder setPass(String pass) {
            try {
                cfg.pass = new URI(pass);
                return this;
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(pass + " is not URI.");
            }
        }

        public LocationConfig build() {
            return cfg;
        }
    }

}
