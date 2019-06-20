package net.unit8.bouncr.hook.license.entity;

import net.unit8.bouncr.entity.User;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "user_licenses")
public class UserLicense implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "user_licence_id")
    private Long id;

    private User user;

    @Column(name = "license_key")
    private String licenseKey;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getLicenseKey() {
        return licenseKey;
    }

    public void setLicenseKey(String licenseKey) {
        this.licenseKey = licenseKey;
    }
}
