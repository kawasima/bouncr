package net.unit8.bouncr.api.boundary;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

public class PasswordCredentialUpdateRequest implements Serializable {
    @NotBlank
    private String oldPassword;
    @NotBlank
    private String newPassword;

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
