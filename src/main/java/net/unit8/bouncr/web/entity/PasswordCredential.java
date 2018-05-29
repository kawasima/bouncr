package net.unit8.bouncr.web.entity;

import net.unit8.bouncr.web.EventDateTime;
import net.unit8.bouncr.web.EventDateTimeEntityListener;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity(listener = EventDateTimeEntityListener.class)
@Table(name = "password_credentials")
public class PasswordCredential implements Serializable {
    @Id
    @Column(name = "user_id")
    private Long id;

    private byte[] password;

    private String salt;

    private boolean initial;
    @EventDateTime
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public byte[] getPassword() {
        return password;
    }

    public void setPassword(byte[] password) {
        this.password = password;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public boolean isInitial() {
        return initial;
    }

    public void setInitial(boolean initial) {
        this.initial = initial;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
