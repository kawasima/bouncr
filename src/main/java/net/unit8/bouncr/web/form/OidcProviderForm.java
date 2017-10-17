package net.unit8.bouncr.web.form;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.unit8.bouncr.web.constraints.StringEnumeration;
import net.unit8.bouncr.web.entity.ResponseType;
import net.unit8.bouncr.web.entity.TokenEndpointAuthMethod;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = false)
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
}
