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
        String password = appConfig.getProperty("email.smtp.password");
        String from = appConfig.getProperty("email.from", "noreply@bankx.com");
        LOG.info("Попытка отправки письма на: " + toEmail);
        LOG.info("SMTP сервер: " + host + ":" + port);
        LOG.info("Используемый логин: " + username);
        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", port);
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.starttls.required", "true");
        properties.put("mail.smtp.ssl.protocols", "TLSv1.2");
        properties.put("mail.smtp.ssl.trust", host);
        properties.put("mail.smtp.connectiontimeout", "5000");
        properties.put("mail.smtp.timeout", "5000");
        properties.put("mail.smtp.writetimeout", "5000");
        properties.put("mail.debug", "true");
        try {
            Session session = Session.getInstance(properties, new Authenticator() {
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
            message.setHeader("Content-Type", "text/plain; charset=UTF-8");
            Transport transport = session.getTransport("smtp");
            try {
                transport.connect(host, Integer.parseInt(port), username, password);
                transport.sendMessage(message, message.getAllRecipients());
                LOG.info("Письмо успешно отправлено на: " + toEmail);
            } finally {
                transport.close();
            }
        } catch (Exception e) {
            LOG.severe("Ошибка при отправке письма: " + e.getMessage());
            e.printStackTrace();
        }
    }
}