package net.unit8.bouncr.util;

import java.net.URI;

public interface UriInterpolator {
    URI interpolate(URI uri, String name, String value);
}

