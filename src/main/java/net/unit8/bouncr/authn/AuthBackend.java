package net.unit8.bouncr.authn;

import io.undertow.server.HttpServerExchange;

import java.security.Principal;

/**
 * @author kawasima
 */
public interface AuthBackend {
    Principal authenticate(HttpServerExchange exchange);
}
