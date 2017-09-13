package net.unit8.bouncr.web.form;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@EqualsAndHashCode(callSuper = true)
public class SignUpForm extends FormBase {
    @NotBlank
    @Size(max = 100)
    private String account;

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Size(max = 100)
    @Email
    private String email;

    private String password;

    private String code;

    private boolean passwordDisabled;

    @AssertTrue(message = "invalid password")
    public boolean isValidPasswordWhenEnabled() {
        if (!passwordDisabled) {
            return password != null
                    && !password.isEmpty()
                    && password.length() >= 8
                    && password.length() <= 256;
        }
        return true;
    }
}
