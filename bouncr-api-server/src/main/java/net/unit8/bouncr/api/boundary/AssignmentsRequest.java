package net.unit8.bouncr.api.boundary;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AssignmentsRequest {
    @JsonProperty("role")
    private String roleName;

    @JsonProperty("group")
    private String groupName;

    @JsonProperty("realm")
    private String realmName;
}
