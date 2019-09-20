package net.unit8.bouncr.api;

import kotowari.restful.DecisionPoint;
import kotowari.restful.data.DefaultResource;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static kotowari.restful.DecisionPoint.HANDLE_UNAUTHORIZED;

public class BouncrBaseResource extends DefaultResource {
    private Map<DecisionPoint, Function<RestContext, ?>> functions;

    public BouncrBaseResource() {
        super();
        Map<DecisionPoint, Function<RestContext, ?>> defaultFunctions = getDefaultFunctions();
        functions = new HashMap<>(defaultFunctions);
        functions.put(HANDLE_UNAUTHORIZED, ctx -> new Problem(null, "Unauthorized", 401, "", null));
    }

    @Override
    public Function<RestContext, ?> getFunction(DecisionPoint decisionPoint) {
        return functions.get(decisionPoint);
    }
}
