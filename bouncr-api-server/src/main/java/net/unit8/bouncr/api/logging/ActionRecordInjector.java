package net.unit8.bouncr.api.logging;

import enkan.data.HttpRequest;
import kotowari.inject.ParameterInjector;

public class ActionRecordInjector implements ParameterInjector<ActionRecord> {
    @Override
    public String getName() {
        return "actionRecord";
    }

    @Override
    public boolean isApplicable(Class<?> type, HttpRequest request) {
        return ActionRecord.class.isAssignableFrom(type);
    }

    @Override
    public ActionRecord getInjectObject(HttpRequest request) {
        return ((ActionRecordable) request).getActionRecord();
    }
}
