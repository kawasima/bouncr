package net.unit8.bouncr.api.boundary;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

public class RoleUpdateRequest implements Serializable {
    @NotBlank
    private String name;
    @NotBlank
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
