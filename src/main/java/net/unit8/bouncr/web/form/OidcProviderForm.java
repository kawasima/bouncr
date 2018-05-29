package net.unit8.bouncr.web.form;

import net.unit8.bouncr.web.constraints.StringEnumeration;
import net.unit8.bouncr.web.entity.ResponseType;
import net.unit8.bouncr.web.entity.TokenEndpointAuthMethod;

import javax.validation.constraints.NotBlank;

public class OidcProviderForm extends FormBase {
    private Long id;

    @NotBlank
    private String name;

    @NotBlank
    private String apiKey;

    @NotBlank
    private String apiSecret;

    @NotBlank
    private String scope;

    @NotBlank
    @StringEnumeration(enumClass = ResponseType.class, accessorMethod = "getName")
    private String responseType;

    @NotBlank
    private String authorizationEndpoint;
    private String tokenEndpoint;

    @NotBlank
    @StringEnumeration(enumClass = TokenEndpointAuthMethod.class, accessorMethod = "getValue")
    private String tokenEndpointAuthMethod;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public void setAuthorizationEndpoint(String authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    public String getTokenEndpointAuthMethod() {
        return tokenEndpointAuthMethod;
    }

    public void setTokenEndpointAuthMethod(String tokenEndpointAuthMethod) {
        this.tokenEndpointAuthMethod = tokenEndpointAuthMethod;
    }
}
