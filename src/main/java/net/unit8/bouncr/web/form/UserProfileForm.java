package net.unit8.bouncr.web.form;

import lombok.Data;

@Data
public class UserProfileForm extends FormBase {
    private Long id;
    private String name;
    private String jsonName;
    private boolean isRequired;
    private boolean isIdentity;
}
