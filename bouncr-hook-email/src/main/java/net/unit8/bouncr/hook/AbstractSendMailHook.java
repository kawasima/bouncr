package net.unit8.bouncr.hook;

import enkan.data.HttpResponse;
import enkan.exception.MisconfigurationException;
import kotowari.component.TemplateEngine;
import kotowari.restful.data.RestContext;
import net.unit8.bouncr.entity.User;
import net.unit8.bouncr.entity.UserProfileField;
import net.unit8.bouncr.entity.UserProfileValue;
import net.unit8.bouncr.entity.UserProfileVerification;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Stream;

public abstract class AbstractSendMailHook implements Hook<RestContext> {
    private MailServerConfig mailServerConfig;
    private String templateName;
    private String fromAddress = "nobody";
    private String fromName = "nobody";
    private String subject;
    private String contentType = "text/html";
    private TemplateEngine templateEngine;

    protected abstract Map<String, Object> createContext(RestContext message);

    private String mergeTemplate(RestContext context) {
        Map<String, Object> ctx = createContext(context);

        HttpResponse response = templateEngine.render(templateName,
                ctx.entrySet().stream()
                        .flatMap(e -> Stream.of(e.getKey(), e.getValue()))
                        .toArray());
        return response.getBodyAsString();
    }

    @Override
    public void run(RestContext context) {
        Properties props = System.getProperties();
        props.put("mail.transport.protocol", mailServerConfig.getTransportProtocol());
        props.put("mail.smtp.port", mailServerConfig.getSmtpPort());
        props.put("mail.smtp.starttls.enable", mailServerConfig.isStarttlsEnable());
        props.put("mail.smtp.auth", mailServerConfig.isSmtpAuth());
        props.put("mail.smtp.connectiontimeout", mailServerConfig.getConnectionTimeout());
        props.put("mail.smtp.timeout", mailServerConfig.getTimeout());

        context.getValue(UserProfileVerification.class).ifPresent(verification -> {
            String name = verification.getUserProfileField().getJsonName();
            if (Objects.equals(name, "email")) {
                UserProfileField emailField = verification.getUserProfileField();
                String email = context.getValue(User.class)
                        .map(User::getUserProfileValues)
                        .map(values -> values.stream()
                                .filter(v -> v.getUserProfileField().equals(emailField))
                                .findAny()
                                .orElse(null))
                        .map(UserProfileValue::getValue)
                        .orElseThrow(() -> new MisconfigurationException(""));

                Session session = Session.getDefaultInstance(props);
                MimeMessage msg = new MimeMessage(session);
                try {
                    msg.setFrom(new InternetAddress(fromAddress, fromName));
                    msg.setRecipient(Message.RecipientType.TO, new InternetAddress(email, email));
                    msg.setSubject(subject);
                    msg.setContent(mergeTemplate(context), contentType);
                } catch (UnsupportedEncodingException e) {
                    throw new MisconfigurationException("", e);
                } catch (MessagingException e) {
                    throw new HookRuntimeException(e);
                }

                try (Transport transport = session.getTransport()) {
                    transport.connect(mailServerConfig.getSmtpHost(),
                            mailServerConfig.getSmtpUsername(),
                            mailServerConfig.getSmtpPassword());
                    transport.sendMessage(msg, msg.getAllRecipients());
                } catch (NoSuchProviderException e) {
                    throw new MisconfigurationException("", e);
                } catch (MessagingException e) {
                    throw new HookRuntimeException(e);
                }
            }
        });

    }

    public void setMailServerConfig(MailServerConfig mailServerConfig) {
        this.mailServerConfig = mailServerConfig;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setTemplateEngine(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }
}