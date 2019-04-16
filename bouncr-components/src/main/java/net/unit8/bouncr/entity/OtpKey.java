package net.unit8.bouncr.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.unit8.bouncr.util.Base32Utils;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "otp_keys")
public class OtpKey implements Serializable {
    @Id
    @OneToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @JsonIgnore
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

    @JsonProperty("key")
    public String getEncodedKey() {
        return key != null ? Base32Utils.encode(key) : null;
    }
}
