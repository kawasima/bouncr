package net.unit8.bouncr.api.boundary;

import java.io.Serializable;
import java.util.Objects;

public class AssignmentRequest implements Serializable {
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

    @Override
    public String toString() {
        return "AssignementRequest {" +
                "group = " + Objects.toString(group) + ", " +
                "role = " + Objects.toString(role) + ", " +
                "realm = " + Objects.toString(realm) +
                "}";
    }

    public static class IdObject implements Serializable {
        private Long id;
        private String name;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "IdObject{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    '}';
        }
    }
}
