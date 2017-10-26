package net.unit8.bouncr.web.form;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

@Data
@EqualsAndHashCode(callSuper = true)
public class ForceChangePasswordForm extends FormBase {
    @NotBlank
    private String account;

    @NotBlank
    private String newPassword;

    @NotBlank
    private String oldPassword;

    private String url;
}
