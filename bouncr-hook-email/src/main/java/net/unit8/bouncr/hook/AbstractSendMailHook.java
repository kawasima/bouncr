package net.unit8.bouncr.hook;

import kotowari.restful.data.RestContext;
import net.unit8.bouncr.entity.UserProfileValue;
import net.unit8.bouncr.hook.email.config.MailConfig;
import net.unit8.bouncr.hook.email.service.SendMailService;

import java.util.Map;
import java.util.Optional;

public abstract class AbstractSendMailHook implements Hook<RestContext> {
    private MailConfig mailConfig;
    protected abstract Map<String, Object> createContext(RestContext message);
    protected abstract String getMetaKey();
    protected abstract Optional<UserProfileValue> findEmailField(RestContext context);

    @Override
    public void run(RestContext context) {
        final SendMailService sendMailService = new SendMailService(mailConfig);
        findEmailField(context).ifPresent(value -> {
            String email = value.getValue();
            sendMailService.send(email, getMetaKey(), createContext(context));
        });
    }

    public void setMailConfig(MailConfig config) {
        mailConfig = config;
    }
}
