package com.bankx.notification.service;

import com.bankx.notification.config.ApplicationConfig;
import com.bankx.notification.exception.ApplicationException;
import com.bankx.notification.exception.ErrorCode;
import com.bankx.notification.model.entity.EmailTemplate;
import com.bankx.notification.repository.EmailTemplateRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.HashMap;
import java.util.Map;
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
    private ApplicationConfig applicationConfig;

    @Inject
    private EmailTemplateRepository emailTemplateRepository;

    /**
     * Заменяет переменные в шаблоне на реальные значения.
     *
     * <p>Переменные в шаблоне должны быть оформлены в двойных фигурных скобках: {{variableName}}
     *
     * @param template  шаблон текста с переменными
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
     * @param toEmail      адрес получателя
     * @param variables    значения для подстановки в шаблон
     * @throws ApplicationException если шаблон не найден или произошла ошибка отправки
     */
    public void sendTemplatedEmail(String templateType, String toEmail, Map<String, String> variables) {
        EmailTemplate template = emailTemplateRepository.findByTemplateType(templateType);
        if (template == null) {
            throw new ApplicationException(
                    ErrorCode.EMAIL_TEMPLATE_NOT_FOUND,
                    "Email template not found",
                    "Template type: " + templateType
            );
        }
        try {
            String subject = processTemplate(template.getSubject(), variables);
            String body = processTemplate(template.getBody(), variables);
            sendEmailWithRetry(toEmail, subject, body, false);
        } catch (Exception e) {
            throw new ApplicationException(
                    ErrorCode.EMAIL_SEND_ERROR,
                    "Failed to process email template",
                    "Template type: " + templateType + ", To: " + toEmail,
                    e
            );
        }
    }

    /**
     * Отправляет письмо активации аккаунта.
     *
     * @param toEmail        адрес получателя
     * @param activationLink ссылка для активации аккаунта
     * @param firstName      имя пользователя
     * @param lastName       фамилия пользователя
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
     * @param toEmail   адрес получателя
     * @param resetLink ссылка для сброса пароля
     * @param firstName имя пользователя
     * @param lastName  фамилия пользователя
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
     * @param toEmail   адрес получателя
     * @param firstName имя пользователя
     * @param lastName  фамилия пользователя
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
     * @param toEmail   адрес получателя
     * @param firstName имя пользователя
     * @param lastName  фамилия пользователя
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
     * @param body    тело письма
     * @param isHtml  флаг, указывающий на HTML-формат письма
     * @throws ApplicationException если не удалось отправить письмо после всех попыток
     */
    private void sendEmailWithRetry(String toEmail, String subject, String body, boolean isHtml) {
        int attempt = 0;
        Exception lastException = null;
        while (attempt < MAX_RETRIES) {
            attempt++;
            try {
                sendEmail(toEmail, subject, body, isHtml);
                LOG.info("Email successfully sent to: " + toEmail + " (attempt " + attempt + ")");
                return; // Успешно отправлено, выходим из метода
            } catch (Exception e) {
                lastException = e;
                LOG.warning("Failed to send email to " + toEmail + " (attempt " + attempt + "): " + e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new ApplicationException(
                                ErrorCode.EMAIL_SEND_ERROR,
                                "Email sending interrupted",
                                "To: " + toEmail,
                                ie
                        );
                    }
                }
            }
        }
        // Если дошли до сюда, значит все попытки не удались
        throw new ApplicationException(
                ErrorCode.EMAIL_SEND_ERROR,
                "Failed to send email after " + MAX_RETRIES + " attempts",
                "To: " + toEmail + ", Subject: " + subject,
                lastException
        );
    }

    /**
     * Отправляет электронное письмо через SMTP-сервер.
     *
     * @param toEmail адрес получателя
     * @param subject тема письма
     * @param body    тело письма
     * @param isHtml  флаг, указывающий на HTML-формат письма
     * @throws ApplicationException если произошла ошибка отправки
     */
    private void sendEmail(String toEmail, String subject, String body, boolean isHtml) {
        try {
            String host = applicationConfig.getProperty("email.smtp.host");
            String port = applicationConfig.getProperty("email.smtp.port");
            String username = applicationConfig.getProperty("email.smtp.username");
            String from = applicationConfig.getProperty("email.from.address", username);
            String password = applicationConfig.getProperty("email.smtp.password");
            Properties properties = new Properties();
            properties.put("mail.smtp.host", host);
            properties.put("mail.smtp.port", port);
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.ssl.enable", "true");
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.ssl.trust", host);
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
            if (isHtml) {
                message.setContent(body, "text/html; charset=UTF-8");
            } else {
                message.setText(body, "UTF-8");
            }
            Transport.send(message);
            LOG.info("Email successfully delivered to SMTP server for: " + toEmail);
        } catch (AuthenticationFailedException e) {
            throw new ApplicationException(
                    ErrorCode.EMAIL_SEND_ERROR,
                    "SMTP authentication failed",
                    "Check email credentials",
                    e
            );
        } catch (MessagingException e) {
            throw new ApplicationException(
                    ErrorCode.EMAIL_SEND_ERROR,
                    "Failed to send email",
                    "To: " + toEmail + ", Subject: " + subject,
                    e
            );
        } catch (Exception e) {
            throw new ApplicationException(
                    ErrorCode.EMAIL_SEND_ERROR,
                    "Unexpected error occurred while sending email",
                    "To: " + toEmail,
                    e
            );
        }
    }
}