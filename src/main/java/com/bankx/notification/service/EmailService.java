package com.bankx.notification.service;

import com.bankx.notification.config.ApplicationConfig;
import com.bankx.notification.exception.ApplicationException;
import com.bankx.notification.exception.ErrorCode;
import com.bankx.notification.model.entity.EmailTemplate;
import com.bankx.notification.repository.EmailTemplateRepository;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

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
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000;
    private Configuration freemarkerConfig;

    @Inject
    private ApplicationConfig applicationConfig;

    @Inject
    private EmailTemplateRepository emailTemplateRepository;

    /**
     * Инициализация FreeMarker конфигурации после создания бина.
     *
     * <p>Настраивает версию, кодировку и обработчик исключений для шаблонов.
     * В дальнейшем используется для обработки шаблонов с переменными.
     */
    @PostConstruct
    public void init() {
        freemarkerConfig = new Configuration(Configuration.VERSION_2_3_32);
        freemarkerConfig.setDefaultEncoding("UTF-8");
        freemarkerConfig.setTemplateExceptionHandler(freemarker.template.TemplateExceptionHandler.RETHROW_HANDLER);
    }

    /**
     * Обрабатывает шаблон текста с переменными, подставляя их в шаблон.
     *
     * <p>Переменные в шаблоне оформлены в виде ${variableName} — синтаксис FreeMarker.
     * Для обработки используется библиотека FreeMarker, позволяющая динамически заменить
     * все такие переменные на соответствующие значения из карты variables.
     *
     * @param templateContent шаблон текста с переменными в формате ${variableName}
     * @param variables       карта значений для подстановки в шаблон
     * @return обработанный текст с подставленными значениями
     */
    private String processTemplate(String templateContent, Map<String, String> variables) {
        try {
            StringTemplateLoader stringLoader = new StringTemplateLoader();
            String templateName = "template";
            stringLoader.putTemplate(templateName, templateContent);
            freemarkerConfig.setTemplateLoader(stringLoader);
            Template template = freemarkerConfig.getTemplate(templateName);
            StringWriter writer = new StringWriter();
            Map<String, Object> templateVariables = new HashMap<>(variables);
            template.process(templateVariables, writer);
            return writer.toString();
        } catch (IOException e) {
            throw new ApplicationException(
                    ErrorCode.EMAIL_TEMPLATE_NOT_FOUND,
                    "Failed to load email template",
                    e.getMessage(),
                    e
            );
        } catch (TemplateException e) {
            throw new ApplicationException(
                    ErrorCode.VALIDATION_ERROR,
                    "Template processing error",
                    e.getMessage(),
                    e
            );
        } catch (Exception e) {
            throw new ApplicationException(
                    ErrorCode.UNKNOWN_ERROR,
                    "Unexpected error during template processing",
                    e.getMessage(),
                    e
            );
        }
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
            LOG.info("Processing template: " + templateType + " for email: " + toEmail);
            String subject = processTemplate(template.getSubject(), variables);
            String body = processTemplate(template.getBody(), variables);
            sendEmailWithRetry(toEmail, subject, body, template.getIsHtml());
        } catch (Exception e) {
            LOG.severe("Failed to process email template: " + e.getMessage());
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
        variables.put("firstName", firstName != null ? firstName : "");
        variables.put("lastName", lastName != null ? lastName : "");
        variables.put("activationLink", activationLink != null ? activationLink : "");
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
        variables.put("firstName", firstName != null ? firstName : "");
        variables.put("lastName", lastName != null ? lastName : "");
        variables.put("resetLink", resetLink != null ? resetLink : "");
        sendTemplatedEmail("password_reset_request", toEmail, variables);
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
                return;
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
            if (host == null || port == null || username == null || password == null) {
                throw new ApplicationException(
                        ErrorCode.EMAIL_SEND_ERROR,
                        "SMTP configuration is incomplete",
                        "Check email configuration properties"
                );
            }
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