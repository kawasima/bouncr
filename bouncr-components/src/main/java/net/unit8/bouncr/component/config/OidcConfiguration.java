package net.unit8.bouncr.component.config;

import net.unit8.bouncr.util.UriInterpolator;
import net.unit8.bouncr.util.interpolator.QueryUriInterpolator;

import java.io.Serializable;
import java.net.URI;

public class OidcConfiguration implements Serializable {
    private UriInterpolator uriInterpolator = new QueryUriInterpolator();
    private URI signUpRedirectUrl;
    private URI signInRedirectUrl;
    private URI unauthenticateRedirectUrl;

    public URI getSignUpRedirectUrl() {
        return signUpRedirectUrl;
    }

    public void setSignUpRedirectUrl(URI signUpRedirectUrl) {
        this.signUpRedirectUrl = signUpRedirectUrl;
    }

    public URI getSignInRedirectUrl() {
        return signInRedirectUrl;
    }

    public void setSignInRedirectUrl(URI signInRedirectUrl) {
        this.signInRedirectUrl = signInRedirectUrl;
    }

    public URI getUnauthenticateRedirectUrl() {
        return unauthenticateRedirectUrl;
    }

    public void setUnauthenticateRedirectUrl(URI unauthenticateRedirectUrl) {
        this.unauthenticateRedirectUrl = unauthenticateRedirectUrl;
    }

    public UriInterpolator getUriInterpolator() {
        return uriInterpolator;
    }

    public void setUriInterpolator(UriInterpolator uriInterpolator) {
        this.uriInterpolator = uriInterpolator;
    }
}
