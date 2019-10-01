package net.unit8.bouncr.util.interpolator;

import enkan.exception.MisconfigurationException;
import net.unit8.bouncr.util.UriInterpolator;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

public class FragmentUriInterpolator implements UriInterpolator {
    @Override
    public URI interpolate(URI uri, String name, String value) {
        String fragment = Optional.ofNullable(uri.getFragment())
                .map(f -> f + ";" + name + "=" + value)
                .orElse(name + "=" + value);
        try {
            return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(),
                    uri.getPath(), uri.getQuery(), fragment);
        } catch (URISyntaxException e) {
            throw new MisconfigurationException("", e);
        }
    }
}
