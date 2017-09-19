package net.unit8.bouncr.web.form;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TwoFactorAuthForm extends FormBase {
    private String enabled;
}
