package net.unit8.bouncr.api.boundary;

import java.net.URI;

public enum BouncrProblem {
    MALFORMED,
    CONFLICT,
    UNPROCESSABLE,
    PASSWORD_MUST_BE_CHANGED,
    ONE_TIME_PASSWORD_IS_NEEDED,
    ACCOUNT_IS_LOCKED,
    OPENID_PROVIDER_RETURNS_ERROR,
    MISMATCH_STATE,
    MISMATCH_NONCE,
    MISSING_SUBJECT;

    public URI problemUri() {
        return URI.create("/bouncr/problem/" + this.name());
    }
}
