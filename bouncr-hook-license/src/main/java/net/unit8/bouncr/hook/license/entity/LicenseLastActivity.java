package net.unit8.bouncr.hook.license.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "license_last_activities")
public class LicenseLastActivity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "license_last_activity_id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_license_id")
    private UserLicense userLicense;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserLicense getUserLicense() {
        return userLicense;
    }

    public void setUserLicense(UserLicense userLicense) {
        this.userLicense = userLicense;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
}
