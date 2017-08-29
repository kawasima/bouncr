package net.unit8.bouncr.web.form;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;

/**
 * @author kawasima
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class UserForm extends FormBase {
    @NotBlank
    @Length(max = 100)
    private String account;

    @NotBlank
    @Length(max = 100)
    private String name;

    @NotBlank
    @Length(max = 100)
    @Email
    private String email;

    @NotBlank
    @Length(min = 8, max = 256)
    private String password;
}
