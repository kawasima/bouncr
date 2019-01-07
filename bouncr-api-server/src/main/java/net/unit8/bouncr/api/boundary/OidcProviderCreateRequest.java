package net.unit8.bouncr.api.boundary;

import net.unit8.bouncr.api.constraints.StringEnumeration;
import net.unit8.bouncr.entity.ResponseType;
import net.unit8.bouncr.entity.TokenEndpointAuthMethod;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

public class OidcProviderCreateRequest implements Serializable {
    @NotBlank
    @Length(max = 100)
    @Pattern(regexp = "^\\w+$")
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
