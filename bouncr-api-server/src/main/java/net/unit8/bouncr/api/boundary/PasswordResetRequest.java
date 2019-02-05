package net.unit8.bouncr.api.boundary;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

public class PasswordResetRequest implements Serializable {
    @NotBlank
    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
