package net.unit8.bouncr.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@IdClass(UserProfileValueId.class)
@Table(name = "user_profile_values")
public class UserProfileValue implements Serializable {
    @Id
    @ManyToOne
    @JoinColumn(name = "user_profile_field_id")
    @JsonProperty("user_profile_field")
    private UserProfileField userProfileField;

    @Id
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    @Column(name = "\"value\"")
    private String value;

    public UserProfileField getUserProfileField() {
        return userProfileField;
    }

    public void setUserProfileField(UserProfileField userProfileField) {
        this.userProfileField = userProfileField;
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
