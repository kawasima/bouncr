package net.unit8.bouncr.web.entity;

import lombok.Data;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.io.Serializable;

@Entity
@Table(name = "otp_keys")
@Data
public class OtpKey implements Serializable {
    @Id
    private Long userId;

    @Column(name = "otp_key")
    private byte[] key;
}
