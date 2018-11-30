package net.unit8.bouncr.web.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "certs")
public class Cert implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cert_id")
    private Long id;
    private Long userId;
    private BigInteger serial;
    private LocalDate expires;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigInteger getSerial() {
        return serial;
    }

    public void setSerial(BigInteger serial) {
        this.serial = serial;
    }

    public LocalDate getExpires() {
        return expires;
    }

    public void setExpires(LocalDate expires) {
        this.expires = expires;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cert cert = (Cert) o;
        return Objects.equals(id, cert.id) &&
                Objects.equals(userId, cert.userId) &&
                Objects.equals(serial, cert.serial) &&
                Objects.equals(expires, cert.expires);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, serial, expires);
    }

    @Override
    public String toString() {
        return "Cert{" +
                "id=" + id +
                ", userId=" + userId +
                ", serial=" + serial +
                ", expires=" + expires +
                '}';
    }
}
