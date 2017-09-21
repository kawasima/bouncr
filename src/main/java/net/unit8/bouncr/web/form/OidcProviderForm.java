package net.unit8.bouncr.web.form;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.unit8.bouncr.web.entity.TokenEndpointAuthMethod;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = false)
public class OidcProviderForm extends FormBase {
    private Long id;

    private String apiKey;
    private String apiSecret;
    private String scope;
    private String responseType;
    private String authorizationEndpoint;
    private String tokenEndpoint;
    private TokenEndpointAuthMethod tokenEndpointAuthMethod;
}
