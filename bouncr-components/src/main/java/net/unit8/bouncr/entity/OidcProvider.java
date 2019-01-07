package net.unit8.bouncr.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "oidc_providers")
public class OidcProvider implements Serializable {
    @Id
    @Column(name = "oidc_provider_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @JsonProperty("api_key")
    @Column(name = "api_key")
    private String apiKey;

    @JsonProperty("api_secret")
    @Column(name = "api_secret")
    private String apiSecret;
    private String scope;

    @JsonProperty("response_type")
    @Column(name = "response_type")
    private ResponseType responseType;

    @JsonProperty("token_endpoint")
    @Column(name = "token_endpoint")
    private String tokenEndpoint;

    @JsonProperty("authorization_endpoint")
    @Column(name = "authorization_endpoint")
    private String authorizationEndpoint;

    @JsonProperty("token_endpoint_auth_method")
    @Column(name = "token_endpoint_auth_method")
    private TokenEndpointAuthMethod tokenEndpointAuthMethod;

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

    public ResponseType getResponseType() {
        return responseType;
    }

    public void setResponseType(ResponseType responseType) {
        this.responseType = responseType;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public void setAuthorizationEndpoint(String authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
    }

    public TokenEndpointAuthMethod getTokenEndpointAuthMethod() {
        return tokenEndpointAuthMethod;
    }

    public void setTokenEndpointAuthMethod(TokenEndpointAuthMethod tokenEndpointAuthMethod) {
        this.tokenEndpointAuthMethod = tokenEndpointAuthMethod;
    }
}