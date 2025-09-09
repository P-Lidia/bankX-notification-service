package com.bankx.notification.service;

import com.bankx.notification.model.dto.UserRegistrationEvent;
import com.bankx.notification.model.dto.UserResetPasswordEvent;
import com.bankx.notification.model.entity.NotificationLog;
import com.bankx.notification.repository.NotificationLogRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Сервис обработки уведомлений для системы BankX.
 *
 * <p>Основные функции:
 * <ul>
 *   <li>Обработка событий регистрации пользователей и отправка активационных писем</li>
 *   <li>Обработка запросов на сброс пароля и отправка соответствующих уведомлений</li>
 *   <li>Логирование всех операций по отправке уведомлений в базу данных</li>
 *   <li>Предотвращение обработки дубликатов событий через механизм eventId</li>
 * </ul>
 */
@ApplicationScoped
public class NotificationService {
    private static final Logger log = Logger.getLogger(NotificationService.class.getName());

    @Inject
    private EmailService emailService;

    @Inject
    private NotificationLogRepository repository;

    @Inject
    private jakarta.validation.Validator validator;

    /**
     * Обрабатывает событие активации пользователя.
     *
     * <p>Метод выполняет следующие действия:
     * <ol>
     *   <li>Валидирует входной обьект по аннотациям из ДТО</li>
     *   <li>Проверяет, не обрабатывалось ли уже событие с данным eventId</li>
     *   <li>Создает запись в логе уведомлений со статусом PROCESSING</li>
     *   <li>Формирует ссылку активации на основе activationKey</li>
     *   <li>Отправляет письмо активации через EmailService</li>
     *   <li>Обновляет статус записи в логе на SENT при успешной отправке</li>
     *   <li>В случае ошибки обновляет статус на FAILED и записывает сообщение об ошибке</li>
     * </ol>
     *
     * @param userEvent событие регистрации пользователя, содержащее необходимые данные
     * @throws RuntimeException если не удалось обработать событие активации
     */
    public void processUserActivation(UserRegistrationEvent userEvent) {
        var violations = validator.validate(userEvent);
        if (!violations.isEmpty()) {
            String msg = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect (Collectors.joining("; "));
            log.warning("Validation failed for UserRegistrationEvent: " + msg);
            throw new RuntimeException("Validation failed: " + msg);
        }

        if (userEvent.getEventId() != null && repository.existsByEventId(userEvent.getEventId())) {
            log.info("Duplicate activation event " + userEvent.getEventId() + " — skip");
            return;
        }
        try {
            NotificationLog logEntry = createActivationLogEntry(userEvent);
            repository.saveNotificationLog(logEntry);
            String activationLink = "https://bankx.com/activate?key=" + userEvent.getActivationKey();
            emailService.sendActivationEmail(
                    userEvent.getEmail(),
                    activationLink,
                    userEvent.getFirstName(),
                    userEvent.getLastName()
            );
            updateLogStatusAsSent(logEntry, userEvent.getEventId());
            log.info("Activation email sent to: " + userEvent.getEmail());
        } catch (Exception e) {
            handleActivationError(userEvent, e);
        }
    }

    /**
     * Обрабатывает событие сброса пароля пользователя.
     *
     * <p>Метод выполняет следующие действия:
     * <ol>
     *   <li>Валидирует входной обьект по аннотациям из ДТО</li>
     *   <li>Проверяет, не обрабатывалось ли уже событие с данным eventId</li>
     *   <li>Создает запись в логе уведомлений со статусом PROCESSING</li>
     *   <li>Отправляет письмо сброса пароля через EmailService</li>
     *   <li>Обновляет статус записи в логе на SENT при успешной отправке</li>
     *   <li>Сохраняет информацию об успешной обработке события</li>
     *   <li>В случае ошибки обновляет статус на FAILED и записывает сообщение об ошибке</li>
     * </ol>
     *
     * @param event событие сброса пароля, содержащее необходимые данные
     * @throws RuntimeException если не удалось обработать событие сброса пароля
     */
    public void processPasswordReset(UserResetPasswordEvent event) {
        var violations = validator.validate(event);
        if (!violations.isEmpty()) {
            String msg = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "));
            log.warning("Validation failed for UserResetPasswordEvent: " + msg);
            throw new RuntimeException("Validation failed: " + msg);
        }

        if (event.getEventId() != null && repository.existsByEventId(event.getEventId())) {
            log.info("Duplicate reset event " + event.getEventId() + " — skip");
            return;
        }
        try {
            NotificationLog logEntry = createPasswordResetLogEntry(event);
            repository.saveNotificationLog(logEntry);
            String resetLink = "https://bankx.com/recover?key=" + event.getResetToken();
            emailService.sendPasswordResetEmail(
                    event.getEmail(),
                    resetLink,
                    event.getFirstName(),
                    event.getLastName()
            );
            updateLogStatusAsSent(logEntry, event.getEventId());
            log.info("Password reset email sent to: " + event.getEmail());
            repository.saveSuccessEvent(event.getEventId(), "USER_PASSWORD_RESET_REQUESTED", event.getEmail());
        } catch (Exception e) {
            handlePasswordResetError(event, e);
        }
    }

    /**
     * Отправляет уведомление об успешной активации аккаунта.
     *
     * <p>Используется после подтверждения активации аккаунта пользователем
     * для отправки подтверждающего уведомления.
     *
     * @param email     электронная почта пользователя
     * @param firstName имя пользователя
     * @param lastName  фамилия пользователя
     * @throws RuntimeException если не удалось отправить уведомление
     */
    public void notifyAccountActivated(String email, String firstName, String lastName) {
        try {
            emailService.sendAccountActivatedEmail(email, firstName, lastName);
            log.info("Account activated notification sent to: " + email);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to send account activated notification for: " + email, e);
            throw new RuntimeException("Failed to send account activated notification", e);
        }
    }

    /**
     * Отправляет уведомление об успешном сбросе пароля.
     *
     * <p>Используется после успешного изменения пароля пользователем
     * для отправки подтверждающего уведомления.
     *
     * @param email     электронная почта пользователя
     * @param firstName имя пользователя
     * @param lastName  фамилия пользователя
     * @throws RuntimeException если не удалось отправить уведомление
     */
    public void notifyPasswordResetSuccess(String email, String firstName, String lastName) {
        try {
            emailService.sendPasswordResetSuccessEmail(email, firstName, lastName);
            log.info("Password reset success notification sent to: " + email);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to send password reset success notification for: " + email, e);
            throw new RuntimeException("Failed to send password reset success notification", e);
        }
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    /**
     * Создает запись лога для события активации пользователя.
     *
     * @param userEvent событие регистрации пользователя
     * @return подготовленная запись лога
     */
    private NotificationLog createActivationLogEntry(UserRegistrationEvent userEvent) {
        NotificationLog logEntry = new NotificationLog();
        logEntry.setEventId(userEvent.getEventId());
        logEntry.setEventType("USER_ACTIVATION");
        logEntry.setEmail(userEvent.getEmail());
        logEntry.setFirstName(userEvent.getFirstName());
        logEntry.setLastName(userEvent.getLastName());
        logEntry.setActivationKey(userEvent.getActivationKey());
        logEntry.setStatus("PROCESSING");
        return logEntry;
    }

    /**
     * Создает запись лога для события сброса пароля.
     *
     * @param event событие сброса пароля
     * @return подготовленная запись лога
     */
    private NotificationLog createPasswordResetLogEntry(UserResetPasswordEvent event) {
        NotificationLog logEntry = new NotificationLog();
        logEntry.setEventId(event.getEventId());
        logEntry.setEventType("PASSWORD_RESET");
        logEntry.setEmail(event.getEmail());
        logEntry.setFirstName(event.getFirstName());
        logEntry.setLastName(event.getLastName());
        logEntry.setResetToken(event.getResetToken());
        logEntry.setStatus("PROCESSING");
        return logEntry;
    }

    /**
     * Обновляет статус записи лога на "SENT".
     *
     * @param logEntry запись лога
     * @param eventId  идентификатор события
     */
    private void updateLogStatusAsSent(NotificationLog logEntry, String eventId) {
        if (logEntry.getId() != null) {
            repository.markNotificationLogAsSent(logEntry.getId());
        } else {
            repository.updateNotificationLogStatusByEventId(eventId, "SENT", null);
        }
    }

    /**
     * Обрабатывает ошибку при активации пользователя.
     *
     * @param userEvent событие регистрации пользователя
     * @param e         исключение, вызвавшее ошибку
     */
    private void handleActivationError(UserRegistrationEvent userEvent, Exception e) {
        log.log(Level.SEVERE, "Failed to process user activation for: " + userEvent.getEmail(), e);
        if (userEvent.getEventId() != null) {
            repository.updateNotificationLogStatusByEventId(
                    userEvent.getEventId(),
                    "FAILED",
                    e.getMessage()
            );
        }
        throw new RuntimeException("Failed to process user activation", e);
    }

    /**
     * Обрабатывает ошибку при сбросе пароля.
     *
     * @param event событие сброса пароля
     * @param e     исключение, вызвавшее ошибку
     */
    private void handlePasswordResetError(UserResetPasswordEvent event, Exception e) {
        log.log(Level.SEVERE, "Failed to process password reset for: " + event.getEmail(), event);
        if (event.getEventId() != null) {
            repository.updateNotificationLogStatusByEventId(
                    event.getEventId(),
                    "FAILED",
                    e.getMessage()
            );
        }
        throw new RuntimeException("Failed to process password reset", e);
    }
}