package net.unit8.bouncr.hook.email.config;

import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import kotowari.component.TemplateEngine;

import java.util.HashMap;
import java.util.Map;

public class MailConfig extends SystemComponent<MailConfig> {
    private MailServerConfig mailServerConfig;
    private Map<String, MailMetaConfig> mailMetaConfigMap;
    private TemplateEngine templateEngine;

    public MailConfig() {
        mailMetaConfigMap = new HashMap<>();
    }

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

    public void setTemplateEngine(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public void putMailMetaConfig(String key, MailMetaConfig mailMetaConfig) {
        mailMetaConfigMap.put(key, mailMetaConfig);
    }

    public MailMetaConfig getMailMetaConfig(String key) {
        return mailMetaConfigMap.get(key);
    }

    public TemplateEngine getTemplateEngine() {
        return templateEngine;
    }
}
