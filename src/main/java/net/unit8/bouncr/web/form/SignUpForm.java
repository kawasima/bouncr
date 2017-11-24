package net.unit8.bouncr.web.form;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.*;

@Data
@EqualsAndHashCode(callSuper = true)
public class SignUpForm extends FormBase implements UserRegisterForm {
    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = "[\\p{Alnum}_]+", message = "{account.Pattern.messages}")
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
