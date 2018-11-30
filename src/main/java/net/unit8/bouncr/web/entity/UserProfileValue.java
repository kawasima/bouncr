package net.unit8.bouncr.web.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "user_profile_values")
public class UserProfileValue implements Serializable {
    @Id
    private Long userProfileFieldId;
    @Id
    @OneToOne
    private User user;
    private String value;

    public Long getUserProfileFieldId() {
        return userProfileFieldId;
    }

    public void setUserProfileFieldId(Long userProfileFieldId) {
        this.userProfileFieldId = userProfileFieldId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
