package net.unit8.bouncr.entity;

import java.io.Serializable;
import java.util.Objects;

public class UserProfileValueId implements Serializable {
    private Long userProfileField;
    private Long user;

    public UserProfileValueId() {}

    public UserProfileValueId(Long userProfileField, Long user) {
        this.userProfileField = userProfileField;
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserProfileValueId)) return false;
        UserProfileValueId that = (UserProfileValueId) o;
        return Objects.equals(userProfileField, that.userProfileField) &&
               Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userProfileField, user);
    }
}
