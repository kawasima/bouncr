package net.unit8.bouncr.web.form;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.Length;

/**
 * @author kawasima
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class UserForm extends FormBase {
    @Length(max = 100)
    private String account;

    @Length(max = 100)
    private String name;

    @Length(max = 100)
    @Email
    private String email;

    @Length(min = 8, max = 256)
    private String password;
}
