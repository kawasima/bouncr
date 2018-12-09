package net.unit8.bouncr.api.resource;

import kotowari.restful.Decision;
import net.unit8.bouncr.api.boundary.ProblemDescription;

import static kotowari.restful.DecisionPoint.HANDLE_OK;

public class ProblemResource {
    @Decision(HANDLE_OK)
    public ProblemDescription handleOk() {
        return new ProblemDescription();
    }
}
