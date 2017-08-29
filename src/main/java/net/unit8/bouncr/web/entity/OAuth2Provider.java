package net.unit8.bouncr.web.entity;

import lombok.Data;
import org.seasar.doma.*;

import java.io.Serializable;

@Entity
@Table(name = "oauth2_providers")
@Data
public class OAuth2Provider implements Serializable {
    @Id
    @Column(name = "oauth2_provider_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String apiKey;
    private String apiSecret;
    private String scope;
    private String state;
    private String responseType;
    private String userAgent;
}
