package net.unit8.bouncr.api.boundary;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.unit8.bouncr.api.constraints.StringEnumeration;
import net.unit8.bouncr.entity.ResponseType;
import net.unit8.bouncr.entity.TokenEndpointAuthMethod;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

public class OidcProviderUpdateRequest implements Serializable {
    @NotBlank
    @Length(max = 100)
    @Pattern(regexp = "^\\w+$")
    private String name;

    @JsonProperty("client_id")
    @NotBlank
    @Length(max = 256)
    private String clientId;

    @JsonProperty("client_secret")
    @NotBlank
    @Length(max = 256)
    private String clientSecret;

    @NotBlank
    @Length(max = 256)
    private String scope;

    @NotBlank
    @JsonProperty("response_type")
    @StringEnumeration(enumClass = ResponseType.class, accessorMethod = "getName")
    @Length(max = 16)
    private String responseType;

    @JsonProperty("authorization_endpoint")
    @NotBlank
    @Length(max = 256)
    private String authorizationEndpoint;

    @JsonProperty("token_endpoint")
    @Length(max = 256)
    private String tokenEndpoint;

    @NotBlank
    @StringEnumeration(enumClass = TokenEndpointAuthMethod.class, accessorMethod = "getValue")
    @JsonProperty("token_endpoint_auth_method")
    private String tokenEndpointAuthMethod;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
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
