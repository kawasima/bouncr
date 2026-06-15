package net.unit8.bouncr.api.inject;

import enkan.data.Extendable;
import enkan.web.data.HttpRequest;
import kotowari.inject.ParameterInjector;
import org.jooq.DSLContext;

public class DSLContextInjector implements ParameterInjector<DSLContext> {
    @Override
    public String getName() {
        return "dslContext";
    }

    @Override
    public boolean isApplicable(Class<?> type) {
        return DSLContext.class.isAssignableFrom(type);
    }

    @Override
    public DSLContext getInjectObject(HttpRequest request) {
        if (request instanceof Extendable e) {
            return e.getExtension("jooqDslContext");
        }
        return null;
    }
}
