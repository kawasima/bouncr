package net.unit8.bouncr.web.form;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * The form object for login/logout.
 *
 * @author kawasima
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class LoginForm extends FormBase {
    private String account;
    private String password;
    private String url;
}
