package net.unit8.bouncr.entity;

import java.io.Serializable;
import java.util.Objects;

public class UserProfileVerificationId implements Serializable {
    private Long userProfileField;
    private Long user;

    public UserProfileVerificationId() {}

    public UserProfileVerificationId(Long userProfileField, Long user) {
        this.userProfileField = userProfileField;
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserProfileVerificationId)) return false;
        UserProfileVerificationId that = (UserProfileVerificationId) o;
        return Objects.equals(userProfileField, that.userProfileField) &&
               Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userProfileField, user);
    }
}
