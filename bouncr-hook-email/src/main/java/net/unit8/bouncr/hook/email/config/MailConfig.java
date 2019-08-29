package net.unit8.bouncr.hook.email.component;

import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import kotowari.component.TemplateEngine;
import net.unit8.bouncr.hook.MailServerConfig;

public class MailConfig extends SystemComponent<MailConfig> {
    private MailServerConfig mailServerConfig;
    private String templateName;
    private String fromAddress = "nobody";
    private String fromName = "nobody";
    private String subject;
    private String contentType = "text/html";
    private TemplateEngine templateEngine;

    public MailServerConfig getMailServerConfig() {
        return mailServerConfig;
    }

    public void setMailServerConfig(MailServerConfig mailServerConfig) {
        this.mailServerConfig = mailServerConfig;
    }

    @Override
    protected ComponentLifecycle<MailConfig> lifecycle() {
        return new ComponentLifecycle<>() {
            @Override
            public void start(MailConfig component) {

            }

            @Override
            public void stop(MailConfig component) {

            }
        };
    }
}
