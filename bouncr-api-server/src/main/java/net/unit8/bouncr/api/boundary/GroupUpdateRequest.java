package net.unit8.bouncr.api.boundary;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.List;

public class GroupUpdateRequest implements Serializable {
    @NotBlank
    @Length(max = 100)
    @Pattern(regexp = "^\\w+$")
    private String name;
    @NotBlank
    private String description;

    private List<String> users;

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

    public List<String> getUsers() {
        return users;
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }
}
