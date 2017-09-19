package net.unit8.bouncr.web.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.seasar.doma.*;

import java.io.Serializable;

@Entity
@Table(name = "oidc_applications")
@Data
@EqualsAndHashCode
public class OidcApplication implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oidc_application_id")
    private Long id;

    private String name;
    private String clientId;
    private String clientSecret;
    private byte[] privateKey;
    private byte[] publicKey;
    private String homeUrl;
    private String callbackUrl;
    private String description;
}
