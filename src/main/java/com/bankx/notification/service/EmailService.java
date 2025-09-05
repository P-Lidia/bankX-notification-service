package com.bankx.notification.service;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import jakarta.mail.Authenticator;                  // [ADDED]
import jakarta.mail.AuthenticationFailedException; // [ADDED]
import jakarta.mail.Message;                       // [ADDED]
import jakarta.mail.MessagingException;            // [ADDED]
import jakarta.mail.PasswordAuthentication;        // [ADDED]
import jakarta.mail.Session;                       // [ADDED]
import jakarta.mail.Transport;                     // [ADDED]
import jakarta.mail.internet.InternetAddress;      // [ADDED]
import jakarta.mail.internet.MimeMessage;          // [ADDED]
import com.bankx.notification.config.ApplicationConfig;

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
        String from = appConfig.getProperty("email.from.address", username);
        String password = appConfig.getProperty("email.smtp.password"); // [ADDED]

        LOG.info("Попытка отправки письма на: " + toEmail);

        try {
            // [ADDED] ---- ВЕТКА 1: если есть пароль — используем простой парольный режим ----
            if (password != null && !password.isEmpty()) {
                Properties properties = new Properties();
                properties.put("mail.smtp.host", host);
                properties.put("mail.smtp.port", port);
                properties.put("mail.smtp.auth", "true");
                properties.put("mail.smtp.ssl.enable", "true");

                properties.put("mail.debug", "true");

                Session session = Session.getInstance(properties, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(from));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
                message.setSubject("Активация аккаунта BankX");

                String emailText = "Уважаемый пользователь,\n\n" +
                        "для активации вашего аккаунта перейдите по ссылке:\n" +
                        activationLink + "\n\n" +
                        "С уважением,\nКоманда BankX";
                message.setText(emailText);

                Transport.send(message);
                LOG.info("Письмо успешно отправлено (password auth) на: " + toEmail);
                return; // [ADDED] выходим — OAuth2 не нужен
            }

            // [KEPT] ---- ВЕТКА 2: ваш исходный OAuth2-блок без изменений по смыслу ----
            LOG.info("Используем OAuth2 аутентификацию");

            Properties properties = new Properties();
            properties.put("mail.smtp.host", host);
            properties.put("mail.smtp.port", port);
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.ssl.enable", "true");
            properties.put("mail.smtp.socketFactory.port", port);
            properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory"); // [KEPT]* строковое значение
            properties.put("mail.smtp.socketFactory.fallback", "false");

            // OAuth2 настройки
            properties.put("mail.smtp.auth.mechanisms", "XOAUTH2");
            properties.put("mail.smtp.sasl.enable", "true");
            properties.put("mail.smtp.sasl.mechanisms", "XOAUTH2");
            properties.put("mail.smtp.sasl.jaas.config",
                    "com.sun.security.sasl.ClientFactory com.sun.mail.imap.IMAPProvider");

            properties.put("mail.debug", "true");

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

            Transport transport = session.getTransport("smtp");
            transport.connect(host, Integer.parseInt(port), username, oauthToken);
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();

            LOG.info("Письмо успешно отправлено (OAuth2) на: " + toEmail);

        } catch (AuthenticationFailedException e) {
            LOG.severe("Ошибка аутентификации SMTP/OAuth2: " + e.getMessage()); // [CHANGED] более общий текст
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
        String from = appConfig.getProperty("email.from.address", username);
        String password = appConfig.getProperty("email.smtp.password"); // [ADDED]

        LOG.info("Попытка отправки письма на: " + toEmail);

        try {
            // [ADDED] ---- ВЕТКА 1: парольный режим, если задан пароль ----
            if (password != null && !password.isEmpty()) {
                Properties properties = new Properties();
                properties.put("mail.smtp.host", host);
                properties.put("mail.smtp.port", port);
                properties.put("mail.smtp.auth", "true");
                properties.put("mail.smtp.ssl.enable", "true");
                properties.put("mail.debug", "true");

                Session session = Session.getInstance(properties, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(from));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
                message.setSubject(subject, "UTF-8");
                message.setContent(htmlBody, "text/html; charset=UTF-8");

                Transport.send(message);
                LOG.info("Письмо успешно отправлено (password auth) на: " + toEmail);
                return; // [ADDED]
            }

            // [KEPT] ---- ВЕТКА 2: исходный OAuth2-блок ----
            LOG.info("Используем OAuth2 аутентификацию");

            Properties properties = new Properties();
            properties.put("mail.smtp.host", host);
            properties.put("mail.smtp.port", port);
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.ssl.enable", "true");
            properties.put("mail.smtp.socketFactory.port", port);
            properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory"); // [KEPT]*
            properties.put("mail.smtp.socketFactory.fallback", "false");

            // OAuth2 настройки
            properties.put("mail.smtp.auth.mechanisms", "XOAUTH2");
            properties.put("mail.smtp.sasl.enable", "true");
            properties.put("mail.smtp.sasl.mechanisms", "XOAUTH2");
            properties.put("mail.smtp.sasl.jaas.config",
                    "com.sun.security.sasl.ClientFactory com.sun.mail.imap.IMAPProvider");

            properties.put("mail.debug", "true");

            Session session = Session.getInstance(properties);

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject, "UTF-8");
            message.setContent(htmlBody, "text/html; charset=UTF-8");

            Transport transport = session.getTransport("smtp");
            transport.connect(host, Integer.parseInt(port), username, oauthToken);
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();

            LOG.info("Письмо успешно отправлено (OAuth2) на: " + toEmail);
        } catch (AuthenticationFailedException e) {
            LOG.severe("Ошибка аутентификации SMTP/OAuth2: " + e.getMessage()); // [CHANGED]
            throw new RuntimeException(e);
        } catch (MessagingException e) {
            LOG.severe("Ошибка отправки письма: " + e.getMessage());
            throw new RuntimeException(e);
        } catch (Exception e) {
            LOG.severe("Неожиданная ошибка: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        String subject = "Сброс пароля";
        String html = "<!doctype html><html><body style='font-family:Arial,sans-serif'>"
                + "<h2>Сброс пароля</h2>"
                + "<p>Здравствуйте!</p>"
                + "<p>Нажмите на ссылку ниже, чтобы установить новый пароль:</p>"
                + "<p><a href='" + resetLink + "'>Сбросить пароль</a></p>"
                + "</body></html>";

        sendPasswordResetEmail(toEmail, subject, html);
    }
}




