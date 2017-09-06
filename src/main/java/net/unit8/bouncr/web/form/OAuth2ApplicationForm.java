package net.unit8.bouncr.web.form;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class OAuth2ApplicationForm extends FormBase {
    private Long id;
    private String homeUrl;
    private String callbackUrl;
    private String description;
}
