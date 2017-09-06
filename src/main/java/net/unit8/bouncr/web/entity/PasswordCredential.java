package net.unit8.bouncr.web.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.io.Serializable;

@Entity
@Table(name = "PASSWORD_CREDENTIALS")
@Data
@EqualsAndHashCode
public class PasswordCredential implements Serializable {
    @Id
    @Column(name = "USER_ID")
    private Long id;

    private byte[] password;

    private String salt;
}
