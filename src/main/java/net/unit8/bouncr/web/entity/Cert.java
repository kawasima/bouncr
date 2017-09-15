package net.unit8.bouncr.web.entity;

import lombok.Data;
import org.seasar.doma.*;

import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "certs")
@Data
public class Cert implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cert_id")
    private Long id;

    private Long userId;

    private String privateKey;
    private String clientCert;
    private LocalDate expires;
}
