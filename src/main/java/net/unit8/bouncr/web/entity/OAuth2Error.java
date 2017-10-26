package net.unit8.bouncr.web.entity;

public enum OAuth2Error {
    INVALID_REQUEST("invalid_request", 400),
    INVALID_CLIENT("invalid_client", 400),
    INVALID_GRANT("invalid_grant", 400),
    UNAUTHORIZED_CLIENT("unauthorized_client", 401),
    UNSUPPORTED_GRANT_TYPE("unsupported_grant_type", 400),
    INVALID_SCOPE("invalid_scope", 400);

    OAuth2Error(String value, int statusCode) {
        this.value = value;
        this.statusCode = statusCode;
    }

    private final String value;
    private final int statusCode;

    public String getValue() {
        return value;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
