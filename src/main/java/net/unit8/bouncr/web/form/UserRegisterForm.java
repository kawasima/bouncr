package net.unit8.bouncr.web.form;

import kotowari.data.Validatable;

public interface UserRegisterForm extends Validatable {
    String getEmail();
    String getAccount();
}
