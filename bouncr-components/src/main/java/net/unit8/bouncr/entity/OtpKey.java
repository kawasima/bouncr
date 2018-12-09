package net.unit8.bouncr.entity;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "otp_keys")
public class OtpKey implements Serializable {
    @Id
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "otp_key")
    private byte[] key;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public byte[] getKey() {
        return key;
    }

    public void setKey(byte[] key) {
        this.key = key;
    }
}
