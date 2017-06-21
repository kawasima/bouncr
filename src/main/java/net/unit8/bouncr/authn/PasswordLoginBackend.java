package net.unit8.bouncr.authn;

import java.security.Principal;

/**
 * @author kawasima
 */
public interface PasswordLoginBackend {
    Principal login(String id, String password);
}
