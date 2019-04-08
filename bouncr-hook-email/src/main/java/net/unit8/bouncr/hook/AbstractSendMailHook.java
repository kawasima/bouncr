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
import javax.mail.internet.MimeUtility;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
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

    protected abstract Optional<UserProfileValue> findEmailField(RestContext context);

    private String mergeTemplate(RestContext context) {
        Map<String, Object> ctx = createContext(context);

        HttpResponse response = templateEngine.render(templateName,
                ctx.entrySet().stream()
                        .flatMap(e -> Stream.of(e.getKey(), e.getValue()))
                        .toArray());
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getBodyAsStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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

        findEmailField(context).ifPresent(value -> {
            String email = value.getValue();

            Session session = Session.getDefaultInstance(props);
            MimeMessage msg = new MimeMessage(session);
            try {
                msg.setFrom(new InternetAddress(fromAddress, MimeUtility.encodeText(fromName, "UTF-8", "B")));
                msg.setRecipient(Message.RecipientType.TO, new InternetAddress(email, email));
                msg.setSubject(MimeUtility.encodeText(subject, "UTF-8", "B"));
                msg.setText(
                        mergeTemplate(context), "UTF-8", contentType);
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
