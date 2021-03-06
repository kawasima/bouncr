package net.unit8.bouncr.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Optional;

@Entity
@Table(name = "user_profile_verifications")
public class UserProfileVerification implements Serializable {
    @Id
    @ManyToOne
    @JoinColumn(name = "user_profile_field_id")
    @JsonProperty("user_profile_field")
    private UserProfileField userProfileField;

    @Id
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String code;

    @JsonProperty("expires_at")
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public boolean equals(Object other) {
        return Optional.ofNullable(other)
                .filter(UserProfileVerification.class::isInstance)
                .map(UserProfileVerification.class::cast)
                .filter(v -> v.getUserProfileField().equals(userProfileField) && v.getUser().equals(user))
                .isPresent();
    }
}
