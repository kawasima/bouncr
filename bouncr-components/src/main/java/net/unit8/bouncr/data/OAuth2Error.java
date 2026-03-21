package net.unit8.bouncr.data;

/**
 * OAuth2/OIDC error codes and their default HTTP status codes.
 *
 * <p>The {@code value} is used for the protocol-level {@code error} response
 * field; {@code statusCode} is the suggested HTTP status for API responses.
 */
public enum OAuth2Error {
    INVALID_REQUEST("invalid_request", 400),
    INVALID_CLIENT("invalid_client", 401),
    INVALID_GRANT("invalid_grant", 400),
    UNAUTHORIZED_CLIENT("unauthorized_client", 401),
    UNSUPPORTED_GRANT_TYPE("unsupported_grant_type", 400),
    INVALID_SCOPE("invalid_scope", 400),
    UNSUPPORTED_RESPONSE_TYPE("unsupported_response_type", 400);

    OAuth2Error(String value, int statusCode) {
        this.value = value;
        this.statusCode = statusCode;
    }

    private final String value;
    private final int statusCode;

    public String getValue() {
        return value;
    }

    /**
     * Returns the default HTTP status code associated with this error.
     */
    public int getStatusCode() {
        return statusCode;
    }
}
