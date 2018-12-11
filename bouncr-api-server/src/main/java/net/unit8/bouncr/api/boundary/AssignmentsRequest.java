package net.unit8.bouncr.api.boundary;

import java.io.Serializable;

public class AssignmentsRequest implements Serializable {
    private IdObject group;
    private IdObject role;
    private IdObject realm;

    public IdObject getGroup() {
        return group;
    }

    public void setGroup(IdObject group) {
        this.group = group;
    }

    public IdObject getRole() {
        return role;
    }

    public void setRole(IdObject role) {
        this.role = role;
    }

    public IdObject getRealm() {
        return realm;
    }

    public void setRealm(IdObject realm) {
        this.realm = realm;
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
