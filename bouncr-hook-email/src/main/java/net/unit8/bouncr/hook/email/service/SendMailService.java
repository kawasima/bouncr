package net.unit8.bouncr.hook.email.service;

import enkan.data.HttpResponse;
import enkan.exception.MisconfigurationException;
import net.unit8.bouncr.hook.HookRuntimeException;
import net.unit8.bouncr.hook.email.config.MailConfig;
import net.unit8.bouncr.hook.email.config.MailMetaConfig;
import net.unit8.bouncr.hook.email.config.MailServerConfig;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SendMailService {
    private MailConfig mailConfig;

    public SendMailService(MailConfig mailConfig) {
        this.mailConfig = mailConfig;
    }

    private String mergeTemplate(Map<String, Object> ctx, MailMetaConfig metaConfig) {
        HttpResponse response = mailConfig.getTemplateEngine().render(metaConfig.getTemplateName(),
                ctx.entrySet().stream()
                        .flatMap(e -> Stream.of(e.getKey(), e.getValue()))
                        .toArray());
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getBodyAsStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void send(String emailAddress, String key, Map<String, Object> context) {
        MailServerConfig mailServerConfig = mailConfig.getMailServerConfig();
        Properties props = System.getProperties();
        props.put("mail.transport.protocol", mailServerConfig.getTransportProtocol());
        props.put("mail.smtp.port", mailServerConfig.getSmtpPort());
        props.put("mail.smtp.starttls.enable", mailServerConfig.isStarttlsEnable());
        props.put("mail.smtp.auth", mailServerConfig.isSmtpAuth());
        props.put("mail.smtp.connectiontimeout", mailServerConfig.getConnectionTimeout());
        props.put("mail.smtp.timeout", mailServerConfig.getTimeout());

        MailMetaConfig metaConfig = mailConfig.getMailMetaConfig(key);
        Session session = Session.getDefaultInstance(props);
        MimeMessage msg = new MimeMessage(session);
        try {
            msg.setFrom(new InternetAddress(metaConfig.getFromAddress(),
                    MimeUtility.encodeText(metaConfig.getFromName(), "UTF-8", "B")));
            msg.setRecipient(Message.RecipientType.TO, new InternetAddress(emailAddress, emailAddress));
            msg.setSubject(MimeUtility.encodeText(metaConfig.getSubject(), "UTF-8", "B"));
            msg.setText(
                    mergeTemplate(context, metaConfig), "UTF-8", metaConfig.getContentType());
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
}
