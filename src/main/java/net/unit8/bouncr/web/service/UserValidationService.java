package net.unit8.bouncr.web.service;

import enkan.component.doma2.DomaProvider;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.web.dao.UserDao;
import net.unit8.bouncr.web.form.UserRegisterForm;

import java.util.Locale;

public class UserValidationService {
    private final DomaProvider daoProvider;
    private final BouncrConfiguration config;

    public UserValidationService(DomaProvider daoProvider, BouncrConfiguration config) {
        this.daoProvider = daoProvider;
        this.config = config;
    }

    public void validate(UserRegisterForm form, Locale locale) {
        UserDao userDao = daoProvider.getDao(UserDao.class);
        userDao.selectOptionallyByAccount(form.getAccount()).ifPresent(u -> form.getErrors().add("account",
                config.getMessageResource().renderMessage(locale, "error.accountDuplicated")));
        userDao.selectOptionallyByEmail(form.getEmail()).ifPresent(u -> form.getErrors().add("email",
                config.getMessageResource().renderMessage(locale, "error.emailDuplicated")));

    }

}
