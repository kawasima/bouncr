package net.unit8.bouncr.api.boundary;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class UserActionSearchParams implements Serializable {
    @JsonProperty("action_type")
    private String actionType;

    private Integer limit;
    private Integer offset;

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }
}
