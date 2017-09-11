package net.unit8.bouncr.web.form;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

@Data
@EqualsAndHashCode(callSuper = true)
public class ChangePasswordForm extends FormBase {
    @NotBlank
    private String newPassword;

    @NotBlank
    private String oldPassword;
}
