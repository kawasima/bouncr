package net.unit8.bouncr.api.logging;

import enkan.data.Extendable;

public interface ActionRecordable extends Extendable {
    default void setActionRecord(ActionRecord actionRecord) {
        setExtension("actionRecord", actionRecord);
    }

    default ActionRecord getActionRecord() {
        return getExtension("actionRecord");
    }
}
