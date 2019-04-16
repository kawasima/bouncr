package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import kotowari.restful.Decision;
import kotowari.restful.data.RestContext;
import net.unit8.bouncr.api.boundary.BouncrProblem;

import static kotowari.restful.DecisionPoint.EXISTS;
import static kotowari.restful.DecisionPoint.HANDLE_OK;

public class ProblemResource {
    @Decision(EXISTS)
    public boolean isExists(Parameters params, RestContext context) {
        try {
            BouncrProblem problem = BouncrProblem.valueOf(params.get("problem").toUpperCase());
            context.putValue(problem);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }

    }

    @Decision(HANDLE_OK)
    public BouncrProblem handleOk(BouncrProblem bouncrProblem) {
        return bouncrProblem;
    }
}
