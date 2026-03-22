package net.unit8.bouncr.api.boundary;

import java.util.List;

/**
 * Response body for sign-out, including OIDC logout propagation results.
 */
public record SignOutResponse(
        List<String> frontchannel_logout_urls,
        BackchannelLogoutSummary backchannel_logout
) {
    public record BackchannelLogoutSummary(
            int attempted,
            int succeeded,
            int failed
    ) {}
}
