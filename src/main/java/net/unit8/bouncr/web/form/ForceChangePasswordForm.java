package net.unit8.bouncr.web.form;

import javax.validation.constraints.NotBlank;

public class ForceChangePasswordForm extends FormBase {
    @NotBlank
    private String account;

    @NotBlank
    private String newPassword;

    @NotBlank
    private String oldPassword;

    private String url;

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
