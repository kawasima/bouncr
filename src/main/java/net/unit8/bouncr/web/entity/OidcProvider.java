package net.unit8.bouncr.web.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.seasar.doma.*;

import java.io.Serializable;

@Entity
@Table(name = "oidc_providers")
@Data
@EqualsAndHashCode
public class OidcProvider implements Serializable {
    @Id
    @Column(name = "oidc_provider_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String apiKey;
    private String apiSecret;
    private String scope;
    private String responseType;
    private String tokenEndpoint;
    private String authorizationEndpoint;

    @Column(name = "token_endpoint_auth_method")
    private TokenEndpointAuthMethod tokenEndpointAuthMethod;
}
