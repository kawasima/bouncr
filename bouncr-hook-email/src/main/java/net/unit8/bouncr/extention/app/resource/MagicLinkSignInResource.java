package net.unit8.bouncr.extention.app.resource;

import kotowari.restful.Decision;
import net.unit8.bouncr.extention.app.boundary.MagicLinkSignInRequest;

import static kotowari.restful.DecisionPoint.*;

public class MagicLinkSignInResource {
    @Decision(POST)
    public void send(MagicLinkSignInRequest createRequest) {

    }
}
