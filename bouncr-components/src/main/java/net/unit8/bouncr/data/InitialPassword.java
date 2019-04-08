package net.unit8.bouncr.data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

public class InitialPassword implements Serializable {
    @NotBlank
    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
