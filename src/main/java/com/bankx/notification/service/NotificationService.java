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
        // идемпотентность (если уже обрабатывали — выходим)
        if (e.getEventId() != null && repository != null && repository.existsByEventId(e.getEventId())) {
            log.info("Duplicate reset event " + e.getEventId() + " — skip");
            return;
        }

        // единообразно с активацией: формируем только ссылку и зовём EmailService
        String resetLink = e.getResetUrl();
        emailService.sendPasswordResetEmail(e.getEmail(), resetLink);

        // фиксируем успешную отправку (для повторов)
        if (e.getEventId() != null && repository != null) {
            repository.saveSuccess(e.getEventId(), "USER_PASSWORD_RESET_REQUESTED", e.getEmail());
        }
    }
}
