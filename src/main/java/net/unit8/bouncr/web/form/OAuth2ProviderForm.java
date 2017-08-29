package net.unit8.bouncr.web.form;

import lombok.Data;

import java.io.Serializable;

@Data
public class OAuth2ProviderForm implements Serializable {
    private Long id;

    private String apiKey;
    private String apiSecret;
    private String scope;
    private String state;
    private String responseType;
    private String userAgent;
}
