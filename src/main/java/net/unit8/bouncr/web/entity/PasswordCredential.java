package net.unit8.bouncr.web.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.unit8.bouncr.web.EventDateTime;
import net.unit8.bouncr.web.EventDateTimeEntityListener;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity(listener = EventDateTimeEntityListener.class)
@Table(name = "PASSWORD_CREDENTIALS")
@Data
@EqualsAndHashCode
public class PasswordCredential implements Serializable {
    @Id
    @Column(name = "USER_ID")
    private Long id;

    private byte[] password;

    private String salt;

    private boolean initial;

    @EventDateTime
    private LocalDateTime createdAt;
}
