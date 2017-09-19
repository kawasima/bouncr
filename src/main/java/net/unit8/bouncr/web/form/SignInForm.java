package net.unit8.bouncr.web.form;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * The form object for signIn/logout.
 *
 * @author kawasima
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SignInForm extends FormBase {
    private String account;
    private String password;
    private String url;
    private String code;
}
