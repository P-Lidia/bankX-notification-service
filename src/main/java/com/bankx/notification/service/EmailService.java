package com.bankx.notification.service;

import com.bankx.notification.config.ApplicationConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.logging.Logger;

@ApplicationScoped
public class EmailService {
    private static final Logger LOG = Logger.getLogger(EmailService.class.getName());

    @Inject
    private ApplicationConfig appConfig;

    public void sendActivationEmail(String toEmail, String activationLink) {
        String host = appConfig.getProperty("email.smtp.host");
        String port = appConfig.getProperty("email.smtp.port");
        String username = appConfig.getProperty("email.smtp.username");
        String oauthToken = appConfig.getProperty("yandex.oauth.token");
        String from = appConfig.getProperty("email.from", username);

        LOG.info("Попытка отправки письма на: " + toEmail);
        LOG.info("Используем OAuth2 аутентификацию");

        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", port);
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.socketFactory.port", port);
        properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        properties.put("mail.smtp.socketFactory.fallback", "false");

        // OAuth2 настройки
        properties.put("mail.smtp.auth.mechanisms", "XOAUTH2");
        properties.put("mail.smtp.sasl.enable", "true");
        properties.put("mail.smtp.sasl.mechanisms", "XOAUTH2");
        properties.put("mail.smtp.sasl.jaas.config",
                "com.sun.security.sasl.ClientFactory com.sun.mail.imap.IMAPProvider");

        properties.put("mail.debug", "true");

        try {
            Session session = Session.getInstance(properties);

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Активация аккаунта BankX");

            String emailText = "Уважаемый пользователь,\n\n" +
                               "для активации вашего аккаунта перейдите по ссылке:\n" +
                               activationLink + "\n\n" +
                               "С уважением,\nКоманда BankX";

            message.setText(emailText);

            LOG.info("Отправляем письмо с OAuth2...");

            // Используем Transport с OAuth2 аутентификацией
            Transport transport = session.getTransport("smtp");
            transport.connect(host, Integer.parseInt(port), username, oauthToken);
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();

            LOG.info("Письмо успешно отправлено на: " + toEmail);

        } catch (AuthenticationFailedException e) {
            LOG.severe("Ошибка аутентификации OAuth2: " + e.getMessage());
            LOG.severe("Проверьте OAuth токен");
        } catch (MessagingException e) {
            LOG.severe("Ошибка отправки письма: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            LOG.severe("Неожиданная ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public void sendPasswordResetEmail(String toEmail, String subject, String htmlBody) {
        String host = appConfig.getProperty("email.smtp.host");
        String port = appConfig.getProperty("email.smtp.port");
        String username = appConfig.getProperty("email.smtp.username");
        String oauthToken = appConfig.getProperty("yandex.oauth.token");
        String from = appConfig.getProperty("email.from", username);

        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", port);
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.socketFactory.port", port);
        properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        properties.put("mail.smtp.socketFactory.fallback", "false");
        properties.put("mail.smtp.auth.mechanisms", "XOAUTH2");
        properties.put("mail.smtp.sasl.enable", "true");
        properties.put("mail.debug", "true");

        try {
            Session session = Session.getInstance(properties);
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject, "UTF-8");
            message.setContent(htmlBody, "text/html; charset=UTF-8"); // ВАЖНО: HTML

            Transport transport = session.getTransport("smtp");
            transport.connect(host, Integer.parseInt(port), username, oauthToken);
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();

            LOG.info("RESET email sent to: " + toEmail);
        } catch (MessagingException e) {
            LOG.severe("RESET email failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}