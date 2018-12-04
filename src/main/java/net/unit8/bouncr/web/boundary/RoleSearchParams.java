package net.unit8.bouncr.web.boundary;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class RoleSearchParams implements Serializable {
    private String name;
    private String description;

    @JsonProperty("write_protected")
    private Boolean writeProtected;
}
