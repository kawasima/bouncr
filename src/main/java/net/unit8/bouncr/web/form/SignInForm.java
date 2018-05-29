package net.unit8.bouncr.web.form;

/**
 * The form object for signIn/logout.
 *
 * @author kawasima
 */
public class SignInForm extends FormBase {
    private String account;
    private String password;
    private String url;
    private String code;

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
