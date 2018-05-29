package net.unit8.bouncr.web.entity;

import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.io.Serializable;

@Entity
@Table(name = "user_profile_values")
public class UserProfileValue implements Serializable {
    @Id
    private Long userProfileFieldId;
    @Id
    private Long userId;
    private String value;

    public Long getUserProfileFieldId() {
        return userProfileFieldId;
    }

    public void setUserProfileFieldId(Long userProfileFieldId) {
        this.userProfileFieldId = userProfileFieldId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
