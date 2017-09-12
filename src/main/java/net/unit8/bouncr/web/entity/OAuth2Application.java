package net.unit8.bouncr.web.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.seasar.doma.*;

import java.io.Serializable;

@Entity
@Table(name = "oauth2_applications")
@Data
@EqualsAndHashCode
public class OAuth2Application implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oauth2_application_id")
    private Long id;

    private String name;
    private String clientId;
    private String clientSecret;
    private String homeUrl;
    private String callbackUrl;
    private String description;
}
