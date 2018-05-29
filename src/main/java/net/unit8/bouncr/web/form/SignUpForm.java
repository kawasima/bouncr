package net.unit8.bouncr.web.form;

import javax.validation.constraints.*;

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

    @Override
    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isPasswordDisabled() {
        return passwordDisabled;
    }

    public void setPasswordDisabled(boolean passwordDisabled) {
        this.passwordDisabled = passwordDisabled;
    }
}
