package net.unit8.bouncr.api.boundary;

import jakarta.validation.constraints.Email;
import java.io.Serializable;
import java.util.List;

public class InvitationCreateRequest implements Serializable {
    @Email
    private String email;

    private List<IdObject> groups;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<IdObject> getGroups() {
        return groups;
    }

    public void setGroups(List<IdObject> groups) {
        this.groups = groups;
    }

    public static class IdObject implements Serializable {
        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

}
