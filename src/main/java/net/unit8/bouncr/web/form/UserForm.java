package net.unit8.bouncr.web.form;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.Map;

/**
 * @author kawasima
 */
public class UserForm extends FormBase implements UserRegisterForm {
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = "[\\p{Alnum}_]+")
    private String account;

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Size(max = 100)
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, max = 256)
    private String password;

    private Map<String, String> profiles;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Map<String, String> getProfiles() {
        return profiles;
    }

    public void setProfiles(Map<String, String> profiles) {
        this.profiles = profiles;
    }
}
