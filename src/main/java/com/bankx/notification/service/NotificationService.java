package com.bankx.notification.service;

import com.bankx.notification.model.dto.UserEvent;
import com.bankx.notification.model.dto.UserPasswordResetRequestedEvent;
import com.bankx.notification.repository.NotificationLogRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.logging.Logger;

@ApplicationScoped
public class NotificationService {
    private static final Logger log = Logger.getLogger(NotificationService.class.getName());
    //todo бизнес  логика  уведомлений - сервис, который не отправляет но знает че мы там делаем при отправке
    @Inject
    private EmailService emailService;

    @Inject
    private NotificationLogRepository repository;

    public void processUserActivation(UserEvent userEvent) {
        String activationLink = "http://bankx.com/activate?key=" + userEvent.getActivationKey();
        emailService.sendActivationEmail(userEvent.getEmail(), activationLink);
    }
    public void processPasswordReset(UserPasswordResetRequestedEvent e) {
        if (e.getEventId() != null && repository != null && repository.existsByEventId(e.getEventId())) {
            log.info("Duplicate reset event " + e.getEventId() + " — skip");
            return;
        }

        String locale = e.getLocale() == null ? "en" : e.getLocale();
        String subject = "Password reset";
        if ("ru".equals(locale)) subject = "Восстановление пароля";
        if ("ar".equals(locale)) subject = "استعادة كلمة المرور";

        String name = e.getFullName();
        String html;
        if ("ru".equals(locale)) {
            html = "<!doctype html><html><body style='font-family:Arial,sans-serif'>" +
                    "<h2>Восстановление пароля</h2>" +
                    "<p>Здравствуйте, " + esc(name) + "!</p>" +
                    "<p>Нажмите ссылку ниже, чтобы задать новый пароль:</p>" +
                    "<p><a href='" + e.getResetUrl() + "'>Сбросить пароль</a></p>" +
                    "</body></html>";
        } else if ("ar".equals(locale)) {
            html = "<!doctype html><html><body style='font-family:Arial,sans-serif;direction:rtl;text-align:right;'>" +
                    "<h2>استعادة كلمة المرور</h2>" +
                    "<p>مرحبًا، " + esc(name) + "!</p>" +
                    "<p>اضغط الرابط أدناه لتعيين كلمة مرور جديدة:</p>" +
                    "<p><a href='" + e.getResetUrl() + "'>إعادة تعيين كلمة المرور</a></p>" +
                    "</body></html>";
        } else {
            html = "<!doctype html><html><body style='font-family:Arial,sans-serif'>" +
                    "<h2>Password reset</h2>" +
                    "<p>Hello, " + esc(name) + "!</p>" +
                    "<p>Click the link below to set a new password:</p>" +
                    "<p><a href='" + e.getResetUrl() + "'>Reset password</a></p>" +
                    "</body></html>";
        }

        emailService.sendPasswordResetEmail(e.getEmail(), subject, html);

        if (e.getEventId() != null && repository != null) {
            repository.saveSuccess(e.getEventId(), "USER_PASSWORD_RESET_REQUESTED", e.getEmail());
        }
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
}

