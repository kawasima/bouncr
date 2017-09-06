package net.unit8.bouncr.web.form;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = false)
public class OAuth2ProviderForm extends FormBase {
    private Long id;

    private String apiKey;
    private String apiSecret;
    private String scope;
    private String state;
    private String responseType;
    private String userAgent;
}
