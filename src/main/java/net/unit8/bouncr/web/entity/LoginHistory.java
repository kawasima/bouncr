package net.unit8.bouncr.web.entity;

import lombok.Data;
import org.seasar.doma.*;

import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "login_histories")
@Data
public class LoginHistory implements Serializable{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "login_id")
    private Long id;

    private String account;
    private Timestamp loginedAt;
    private Boolean successful;
}
