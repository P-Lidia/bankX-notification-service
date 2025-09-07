package com.bankx.notification.service;

import com.bankx.notification.model.entity.EmailTemplate;
import com.bankx.notification.repository.EmailTemplateRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import com.bankx.notification.config.ApplicationConfig;

import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Сервис для отправки электронных писем с использованием шаблонов из MongoDB.
 *
 * <p>Основные функции:
 * <ul>
 *   <li>Отправка различных типов уведомлений (активация, сброс пароля и т.д.)</li>
 *   <li>Использование шаблонов из MongoDB с поддержкой переменных</li>
 *   <li>Повторные попытки отправки при возникновении ошибок</li>
 *   <li>Поддержка как текстовых, так и HTML-писем</li>
 *   <li>Интеграция с SMTP-сервером (включая специальные настройки для Mail.ru)</li>
 * </ul>
 */
@ApplicationScoped
public class EmailService {
    private static final Logger LOG = Logger.getLogger(EmailService.class.getName());
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000;

    @Inject
    private ApplicationConfig appConfig;

    @Inject
    private EmailTemplateRepository templateRepository;

    /**
     * Заменяет переменные в шаблоне на реальные значения.
     *
     * <p>Переменные в шаблоне должны быть оформлены в двойных фигурных скобках: {{variableName}}
     *
     * @param template шаблон текста с переменными
     * @param variables карта значений для подстановки в шаблон
     * @return обработанный текст с подставленными значениями
     */
    private String processTemplate(String template, Map<String, String> variables) {
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String variableName = matcher.group(1);
            String replacement = variables.get(variableName);
            matcher.appendReplacement(result, replacement != null ? replacement : "");
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Отправляет электронное письмо на основе шаблона из MongoDB.
     *
     * @param templateType тип шаблона (регистрация, сброс пароля и т.д.)
     * @param toEmail адрес получателя
     * @param variables значения для подстановки в шаблон
     * @throws RuntimeException если шаблон не найден или произошла ошибка отправки
     */
    public void sendTemplatedEmail(String templateType, String toEmail, Map<String, String> variables) {
        EmailTemplate template = templateRepository.findByTemplateType(templateType);

        if (template == null) {
            throw new RuntimeException("Template not found: " + templateType);
        }

        String subject = processTemplate(template.getSubject(), variables);
        String body = processTemplate(template.getBody(), variables);

        sendEmailWithRetry(toEmail, subject, body, false);
    }

    /**
     * Отправляет письмо активации аккаунта.
     *
     * @param toEmail адрес получателя
     * @param activationLink ссылка для активации аккаунта
     * @param firstName имя пользователя
     * @param lastName фамилия пользователя
     */
    public void sendActivationEmail(String toEmail, String activationLink, String firstName, String lastName) {
        Map<String, String> variables = new HashMap<>();
        variables.put("firstName", firstName);
        variables.put("lastName", lastName);
        variables.put("activationLink", activationLink);

        sendTemplatedEmail("registration", toEmail, variables);
    }

    /**
     * Отправляет письмо с запросом на сброс пароля.
     *
     * @param toEmail адрес получателя
     * @param resetLink ссылка для сброса пароля
     * @param firstName имя пользователя
     * @param lastName фамилия пользователя
     */
    public void sendPasswordResetEmail(String toEmail, String resetLink, String firstName, String lastName) {
        Map<String, String> variables = new HashMap<>();
        variables.put("firstName", firstName);
        variables.put("lastName", lastName);
        variables.put("resetLink", resetLink);

        sendTemplatedEmail("password_reset_request", toEmail, variables);
    }

    /**
     * Отправляет уведомление об успешном сбросе пароля.
     *
     * @param toEmail адрес получателя
     * @param firstName имя пользователя
     * @param lastName фамилия пользователя
     */
    public void sendPasswordResetSuccessEmail(String toEmail, String firstName, String lastName) {
        Map<String, String> variables = new HashMap<>();
        variables.put("firstName", firstName);
        variables.put("lastName", lastName);

        sendTemplatedEmail("password_reset_success", toEmail, variables);
    }

    /**
     * Отправляет уведомление об успешной активации аккаунта.
     *
     * @param toEmail адрес получателя
     * @param firstName имя пользователя
     * @param lastName фамилия пользователя
     */
    public void sendAccountActivatedEmail(String toEmail, String firstName, String lastName) {
        Map<String, String> variables = new HashMap<>();
        variables.put("firstName", firstName);
        variables.put("lastName", lastName);

        sendTemplatedEmail("account_activated", toEmail, variables);
    }

    /**
     * Отправляет письмо с повторными попытками при возникновении ошибок.
     *
     * @param toEmail адрес получателя
     * @param subject тема письма
     * @param body тело письма
     * @param isHtml флаг, указывающий на HTML-формат письма
     */
    private void sendEmailWithRetry(String toEmail, String subject, String body, boolean isHtml) {
        int attempt = 0;
        boolean success = false;

        while (attempt < MAX_RETRIES && !success) {
            attempt++;
            try {
                sendEmail(toEmail, subject, body, isHtml);
                success = true;
                LOG.info("Email successfully sent to: " + toEmail + " (attempt " + attempt + ")");
            } catch (Exception e) {
                LOG.warning("Failed to send email to " + toEmail + " (attempt " + attempt + "): " + e.getMessage());

                if (attempt >= MAX_RETRIES) {
                    LOG.severe("All " + MAX_RETRIES + " attempts failed for: " + toEmail);
                    throw new RuntimeException("Failed to send email after " + MAX_RETRIES + " attempts", e);
                }

                // Задержка перед повторной попыткой
                try {
                    Thread.sleep(RETRY_DELAY_MS * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Email sending interrupted", ie);
                }
            }
        }
    }

    /**
     * Отправляет электронное письмо через SMTP-сервер.
     *
     * @param toEmail адрес получателя
     * @param subject тема письма
     * @param body тело письма
     * @param isHtml флаг, указывающий на HTML-формат письма
     */
    private void sendEmail(String toEmail, String subject, String body, boolean isHtml) {
        String host = appConfig.getProperty("email.smtp.host");
        String port = appConfig.getProperty("email.smtp.port");
        String username = appConfig.getProperty("email.smtp.username");
        String from = appConfig.getProperty("email.from.address", username);
        String password = appConfig.getProperty("email.smtp.password");

        LOG.info("Attempting to send email to: " + toEmail + " from: " + from);

        // Добавляем задержку между отправками для избежания блокировки
        try {
            Thread.sleep(1000 + (long)(Math.random() * 1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Email sending interrupted", e);
        }

        try {
            Properties properties = new Properties();
            properties.put("mail.smtp.host", host);
            properties.put("mail.smtp.port", port);
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.ssl.enable", "true");
            properties.put("mail.smtp.starttls.enable", "true"); // Для Mail.ru
            properties.put("mail.smtp.ssl.trust", host); // Доверяем хосту
            properties.put("mail.debug", "true");

            // Особые настройки для Mail.ru
            properties.put("mail.smtp.socketFactory.port", port);
            properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            properties.put("mail.smtp.socketFactory.fallback", "false");

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

            // Добавляем важные заголовки для уменьшения вероятности попадания в спам
            message.addHeader("Precedence", "bulk");
            message.addHeader("X-Mailer", "BankX Notification Service");
            message.addHeader("List-Unsubscribe", "<mailto:support@bankx.com>");

            if (isHtml) {
                message.setContent(body, "text/html; charset=UTF-8");
            } else {
                message.setText(body, "UTF-8");
            }

            Transport.send(message);
            LOG.info("Email successfully delivered to SMTP server for: " + toEmail);
        } catch (AuthenticationFailedException e) {
            LOG.severe("SMTP authentication error: " + e.getMessage());
            throw new RuntimeException("SMTP authentication failed", e);
        } catch (MessagingException e) {
            LOG.severe("Email sending error: " + e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        } catch (Exception e) {
            LOG.severe("Unexpected error: " + e.getMessage());
            throw new RuntimeException("Unexpected error occurred", e);
        }
    }
}