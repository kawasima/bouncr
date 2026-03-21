package net.unit8.bouncr.api.util;

import enkan.data.HttpRequest;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public final class CookieUtils {
    private CookieUtils() {}

    public static Map<String, String> parseCookies(HttpRequest request) {
        if (request.getHeaders() == null) return Map.of();
        String cookieHeader = request.getHeaders().get("Cookie");
        if (cookieHeader == null) return Map.of();
        return Arrays.stream(cookieHeader.split(";"))
                .map(String::strip)
                .filter(s -> s.contains("="))
                .collect(Collectors.toMap(
                        s -> s.substring(0, s.indexOf('=')),
                        s -> s.substring(s.indexOf('=') + 1),
                        (a, b) -> b));
    }
}
