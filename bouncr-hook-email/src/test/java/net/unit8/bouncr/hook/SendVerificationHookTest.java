package net.unit8.bouncr.hook;

import enkan.component.thymeleaf.ThymeleafTemplateEngine;
import enkan.config.EnkanSystemFactory;
import enkan.data.DefaultHttpRequest;
import enkan.system.EnkanSystem;
import kotowari.restful.data.DefaultResource;
import kotowari.restful.data.RestContext;
import net.unit8.bouncr.entity.User;
import net.unit8.bouncr.entity.UserProfileField;
import net.unit8.bouncr.entity.UserProfileValue;
import net.unit8.bouncr.entity.UserProfileVerification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static enkan.util.BeanBuilder.builder;

public class SendVerificationHookTest {
    private EnkanSystem system;

    @BeforeEach
    public void setup() {
        system = EnkanSystem.of(
                "template", new ThymeleafTemplateEngine()
        );
        system.start();
    }

    @Test
    public void test() {
        RestContext context = new RestContext(new DefaultResource(), new DefaultHttpRequest());
        UserProfileField emailField = builder(new UserProfileField())
                .set(UserProfileField::setId, 1L)
                .set(UserProfileField::setName, "Email")
                .set(UserProfileField::setJsonName, "email")
                .build();

        User user = builder(new User())
                .set(User::setId, 1L)
                .set(User::setAccount, "user1")
                .set(User::setUserProfileValues, List.of(
                        builder(new UserProfileValue())
                                .set(UserProfileValue::setValue, "6d0d543b7e-1df05b@inbox.mailtrap.io")
                                .set(UserProfileValue::setUserProfileField, emailField)
                                .build()
                ))
                .build();

        UserProfileVerification verification = builder(new UserProfileVerification())
                .set(UserProfileVerification::setUserProfileField, emailField)
                .set(UserProfileVerification::setCode, "ABC123")
                .build();
        context.putValue(user);
        context.putValue(verification);

        MailServerConfig mailServerConfig = builder(new MailServerConfig())
                .set(MailServerConfig::setSmtpAuth, true)
                .set(MailServerConfig::setSmtpHost, "smtp.mailtrap.io")
                .set(MailServerConfig::setSmtpPort, 2525)
                .set(MailServerConfig::setSmtpUsername, "fccd8e8584677a")
                .set(MailServerConfig::setSmtpPassword, "f97a92e5f39e35")
                .build();
        SendVerificationHook hook = builder(new SendVerificationHook())
                .set(SendVerificationHook::setContentType, "text/html")
                .set(SendVerificationHook::setMailServerConfig, mailServerConfig)
                .set(SendVerificationHook::setTemplateEngine, system.getComponent("template"))
                .build();

        hook.run(context);
    }

    @AfterEach
    public void tearDown() {
        if (system != null) {
            system.stop();
        }
    }
}
