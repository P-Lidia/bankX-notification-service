package com.bankx.notification.service;

import com.bankx.notification.model.dto.UserEvent;
import com.bankx.notification.model.dto.UserPasswordResetRequestedEvent;
import com.bankx.notification.model.entity.NotificationLog;
import com.bankx.notification.repository.NotificationLogRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.logging.Level;
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
        // Проверяем дубликат по eventId
        if (userEvent.getEventId() != null && repository.existsByEventId(userEvent.getEventId())) {
            log.info("Duplicate activation event " + userEvent.getEventId() + " — skip");
            return;
        }

        try {
            // Создаем лог-запись
            NotificationLog logEntry = new NotificationLog();
            logEntry.setEventId(userEvent.getEventId());
            logEntry.setEventType("USER_ACTIVATION");
            logEntry.setEmail(userEvent.getEmail());
            logEntry.setFirstName(userEvent.getFirstName());
            logEntry.setLastName(userEvent.getLastName());
            logEntry.setActivationKey(userEvent.getActivationKey());
            logEntry.setStatus("PROCESSING");

            // Сохраняем в базу
            repository.save(logEntry);

            // Отправляем email
            String activationLink = "http://bankx.com/activate?key=" + userEvent.getActivationKey();
            emailService.sendActivationEmail(userEvent.getEmail(), activationLink);

            // Обновляем статус на успешный
            if (logEntry.getId() != null) {
                repository.markAsSent(logEntry.getId());
            } else {
                // Если ID не сохранился, используем eventId для обновления
                repository.updateStatusByEventId(userEvent.getEventId(), "SENT", null);
            }

            log.info("Activation email sent to: " + userEvent.getEmail());

        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to process user activation for: " + userEvent.getEmail(), e);

            // Сохраняем ошибку в базу
            if (userEvent.getEventId() != null) {
                repository.updateStatusByEventId(
                        userEvent.getEventId(),
                        "FAILED",
                        e.getMessage()
                );
            }
            throw new RuntimeException("Failed to process user activation", e);
        }
    }
    public void processPasswordReset(UserPasswordResetRequestedEvent event) {
        // идемпотентность (если уже обрабатывали — выходим)
        if (event.getEventId() != null && repository != null && repository.existsByEventId(event.getEventId())) {
            log.info("Duplicate reset event " + event.getEventId() + " — skip");
            return;
        }

        try {
            // Создаем лог-запись
            NotificationLog logEntry = new NotificationLog();
            logEntry.setEventId(event.getEventId());
            logEntry.setEventType("PASSWORD_RESET");
            logEntry.setEmail(event.getEmail());
            logEntry.setFirstName(event.getFirstName());
            logEntry.setLastName(event.getLastName());
            logEntry.setResetToken(event.getResetToken());
            logEntry.setStatus("PROCESSING");

            // Сохраняем в базу
            repository.save(logEntry);

            // единообразно с активацией: формируем только ссылку и зовём EmailService
            String resetLink = event.getResetUrl();
            emailService.sendPasswordResetEmail(event.getEmail(), resetLink);

            // Обновляем статус на успешный
            if (logEntry.getId() != null) {
                repository.markAsSent(logEntry.getId());
            } else {
                repository.updateStatusByEventId(event.getEventId(), "SENT", null);
            }

            log.info("Password reset email sent to: " + event.getEmail());

            // фиксируем успешную отправку (для повторов)
            if (event.getEventId() != null && repository != null) {
                repository.saveSuccess(event.getEventId(), "USER_PASSWORD_RESET_REQUESTED", event.getEmail());
            }

        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to process password reset for: " + event.getEmail(), event);

            // Сохраняем ошибку в базу
            if (event.getEventId() != null) {
                repository.updateStatusByEventId(
                        event.getEventId(),
                        "FAILED",
                        e.getMessage()
                );
            }
            throw new RuntimeException("Failed to process password reset", e);
        }
    }
}
