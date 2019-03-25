package net.unit8.bouncr.api.logging;

import net.unit8.bouncr.entity.ActionType;

import java.io.Serializable;

public class ActionRecord implements Serializable {
    private ActionType actionType;
    private String actor;
    private String description;

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean shouldBeRecorded() {
        return actionType != null;
    }
}
