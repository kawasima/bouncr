package net.unit8.bouncr.web.form;

import javax.validation.constraints.NotBlank;

public class ChangePasswordForm extends FormBase {
    @NotBlank
    private String newPassword;

    @NotBlank
    private String oldPassword;

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }
}
