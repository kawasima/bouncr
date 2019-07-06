package net.unit8.bouncr.hook.license.entity;

import net.unit8.bouncr.entity.User;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "user_licenses")
public class UserLicense implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_license_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "license_key")
    private byte[] licenseKey;

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

    public byte[] getLicenseKey() {
        return licenseKey;
    }

    public void setLicenseKey(byte[] licenseKey) {
        this.licenseKey = licenseKey;
    }
}
