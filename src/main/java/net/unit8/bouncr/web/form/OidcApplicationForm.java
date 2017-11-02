package net.unit8.bouncr.web.form;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class OidcApplicationForm extends FormBase {
    private Long id;

    @NotBlank
    private String name;

    @NotBlank
    private String homeUrl;

    @NotBlank
    private String callbackUrl;

    @NotBlank
    private String description;

    private List<Long> permissionId;
}
