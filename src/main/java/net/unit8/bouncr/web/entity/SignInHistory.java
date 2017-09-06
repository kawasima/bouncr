package net.unit8.bouncr.web.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.seasar.doma.*;

import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "sign_in_histories")
@Data
@EqualsAndHashCode
public class SignInHistory implements Serializable{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sign_in_id")
    private Long id;

    private String account;
    private Timestamp signedInAt;
    private Boolean successful;
}
