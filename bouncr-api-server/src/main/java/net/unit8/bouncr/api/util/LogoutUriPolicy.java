package net.unit8.bouncr.api.util;

import java.net.InetAddress;
import java.net.URI;
import java.util.Locale;

public final class LogoutUriPolicy {
    private LogoutUriPolicy() {}

    public static String normalizeLogoutUri(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        URI uri = URI.create(raw.trim());
        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException("logout URI must be absolute");
        }
        String scheme = uri.getScheme();
        if (scheme == null ||
                (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            throw new IllegalArgumentException("logout URI scheme must be http or https");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("logout URI host is required");
        }
        return uri.toString();
    }

    public static String normalizeBackchannelLogoutUri(String raw) {
        String normalized = normalizeLogoutUri(raw);
        if (normalized == null) return null;

        URI uri = URI.create(normalized);
        if (!isAllowedBackchannelTarget(uri)) {
            throw new IllegalArgumentException("backchannel_logout_uri is not allowed");
        }
        return normalized;
    }

    public static boolean isAllowedBackchannelTarget(URI uri) {
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return false;
        }
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if ("localhost".equals(normalizedHost) || normalizedHost.endsWith(".localhost")) {
            return false;
        }

        if (isIpLiteral(host)) {
            try {
                InetAddress addr = InetAddress.getByName(host);
                return !(addr.isAnyLocalAddress()
                        || addr.isLoopbackAddress()
                        || addr.isSiteLocalAddress()
                        || addr.isLinkLocalAddress()
                        || addr.isMulticastAddress());
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    private static boolean isIpLiteral(String host) {
        if (host.indexOf(':') >= 0) {
            return true;
        }
        for (int i = 0; i < host.length(); i++) {
            char c = host.charAt(i);
            if (!(Character.isDigit(c) || c == '.')) {
                return false;
            }
        }
        return host.contains(".");
    }
}
