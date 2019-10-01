package net.unit8.bouncr.util.interpolator;

import enkan.exception.MisconfigurationException;
import net.unit8.bouncr.util.UriInterpolator;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

public class QueryUriInterpolator implements UriInterpolator {
    @Override
    public URI interpolate(URI uri, String name, String value) {
        String query = Optional.ofNullable(uri.getQuery())
                .map(q -> q + "&" + name + "=" + value)
                .orElse(name + "=" + value);
        try {
            return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(),
                    uri.getPath(), query, uri.getFragment());
        } catch (URISyntaxException e) {
            throw new MisconfigurationException("", e);
        }
    }
}
