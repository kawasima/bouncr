package net.unit8.bouncr.entity;

import java.io.Serializable;
import java.util.Objects;

public class AssignmentId implements Serializable {
    private Long group;
    private Long role;
    private Long realm;

    public AssignmentId() {}

    public AssignmentId(Long group, Long role, Long realm) {
        this.group = group;
        this.role = role;
        this.realm = realm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AssignmentId)) return false;
        AssignmentId that = (AssignmentId) o;
        return Objects.equals(group, that.group) &&
               Objects.equals(role, that.role) &&
               Objects.equals(realm, that.realm);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, role, realm);
    }
}
