package com.bankx.notification.service;

import com.bankx.notification.config.ApplicationConfig;
import com.bankx.notification.exception.ApplicationException;
import com.bankx.notification.exception.ErrorCode;
import com.bankx.notification.exception.ExceptionMapper;
import com.bankx.notification.model.dto.UserRegistrationEvent;
import com.bankx.notification.model.dto.UserResetPasswordEvent;
import com.bankx.notification.model.entity.NotificationLog;
import com.bankx.notification.repository.NotificationLogRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
 * </ul>
 */
@ApplicationScoped
public class NotificationService {
    private static final Logger LOG = Logger.getLogger(NotificationService.class.getName());

    @Inject
    private jakarta.validation.Validator validator;

    @Inject
    private EmailService emailService;

    @Inject
    private NotificationLogRepository notificationLogRepository;

    @Inject
    private ExceptionMapper exceptionMapper;

    @Inject
    private ApplicationConfig applicationConfig;

    /**
     * Обрабатывает событие активации пользователя.
     *
     * <p>Метод выполняет следующие действия:
     * <ol>
     *   <li>Валидирует входной объект по аннотациям из ДТО</li>
     *   <li>Создает запись в логе уведомлений со статусом PROCESSING</li>
     *   <li>Формирует ссылку активации на основе activationKey</li>
     *   <li>Отправляет письмо активации через EmailService</li>
     *   <li>Обновляет статус записи в логе на SENT при успешной отправке</li>
     *   <li>В случае ошибки обновляет статус на FAILED и записывает сообщение об ошибке</li>
     * </ol>
     *
     * @param userEvent событие регистрации пользователя, содержащее необходимые данные
     * @throws ApplicationException если не удалось обработать событие активации
     */
    public void processUserActivation(UserRegistrationEvent userEvent) {
        var violations = validator.validate(userEvent);
        if (!violations.isEmpty()) {
            String msg = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "));
            LOG.warning("Validation failed for UserRegistrationEvent: " + msg);
            throw new ApplicationException(ErrorCode.VALIDATION_ERROR,
                    "Validation failed", msg);
        }

        NotificationLog savedLog = null;
        try {
            NotificationLog logEntry = createActivationLogEntry(userEvent);
            savedLog = notificationLogRepository.saveNotificationLog(logEntry);

            String activationLink = applicationConfig.getAppHost() + "/activate?key=" + userEvent.getActivationKey();
            emailService.sendActivationEmail(
                    userEvent.getEmail(),
                    activationLink,
                    userEvent.getFirstName(),
                    userEvent.getLastName()
            );

            notificationLogRepository.markNotificationLogAsSent(savedLog.getId());
            LOG.info("Activation email sent to: " + userEvent.getEmail());
        } catch (Exception e) {
            if (savedLog != null) {
                notificationLogRepository.updateNotificationLogStatus(
                        savedLog.getId(), "FAILED", e.getMessage());
            } else {
                // Если не удалось сохранить лог, пытаемся обновить по email и activationKey
                notificationLogRepository.updateNotificationLogStatus(
                        userEvent.getEmail(),
                        userEvent.getActivationKey().toString(),
                        "FAILED",
                        e.getMessage()
                );
            }

            if (e instanceof ApplicationException) {
                throw (ApplicationException) e;
            } else {
                throw new ApplicationException(
                        ErrorCode.EMAIL_SEND_ERROR,
                        "Failed to process user activation",
                        "User: " + userEvent.getEmail(),
                        e
                );
            }
        }
    }

    /**
     * Обрабатывает событие сброса пароля пользователя.
     *
     * <p>Метод выполняет следующие действия:
     * <ol>
     *   <li>Валидирует входной объект по аннотациям из ДТО</li>
     *   <li>Создает запись в логе уведомлений со статусом PROCESSING</li>
     *   <li>Отправляет письмо сброса пароля через EmailService</li>
     *   <li>Обновляет статус записи в логе на SENT при успешной отправке</li>
     *   <li>В случае ошибки обновляет статус на FAILED и записывает сообщение об ошибке</li>
     * </ol>
     *
     * @param event событие сброса пароля, содержащее необходимые данные
     * @throws ApplicationException если не удалось обработать событие сброса пароля
     */
    public void processPasswordReset(UserResetPasswordEvent event) {
        var violations = validator.validate(event);
        if (!violations.isEmpty()) {
            String msg = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "));
            LOG.warning("Validation failed for UserResetPasswordEvent: " + msg);
            throw new ApplicationException(ErrorCode.VALIDATION_ERROR,
                    "Validation failed", msg);
        }

        NotificationLog savedLog = null;
        try {
            NotificationLog logEntry = createPasswordResetLogEntry(event);
            savedLog = notificationLogRepository.saveNotificationLog(logEntry);

            String resetLink = applicationConfig.getAppHost() + "/recover?key=" + event.getResetToken();
            emailService.sendPasswordResetEmail(
                    event.getEmail(),
                    resetLink,
                    event.getFirstName(),
                    event.getLastName()
            );

            notificationLogRepository.markNotificationLogAsSent(savedLog.getId());
            LOG.info("Password reset email sent to: " + event.getEmail());
        } catch (Exception e) {
            if (savedLog != null) {
                notificationLogRepository.updateNotificationLogStatus(
                        savedLog.getId(), "FAILED", e.getMessage());
            } else {
                // Если не удалось сохранить лог, пытаемся обновить по email и resetToken
                notificationLogRepository.updateNotificationLogStatusByResetToken(
                        event.getEmail(),
                        event.getResetToken(),
                        "FAILED",
                        e.getMessage()
                );
            }

            if (e instanceof ApplicationException) {
                throw (ApplicationException) e;
            } else {
                throw new ApplicationException(
                        ErrorCode.EMAIL_SEND_ERROR,
                        "Failed to process password reset",
                        "User: " + event.getEmail(),
                        e
                );
            }
        }
    }

    // Вспомогательные методы

    /**
     * Создает запись лога для события активации пользователя.
     *
     * @param userEvent событие регистрации пользователя
     * @return подготовленная запись лога
     */
    private NotificationLog createActivationLogEntry(UserRegistrationEvent userEvent) {
        NotificationLog logEntry = new NotificationLog();
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
        logEntry.setEventType("PASSWORD_RESET");
        logEntry.setEmail(event.getEmail());
        logEntry.setFirstName(event.getFirstName());
        logEntry.setLastName(event.getLastName());
        logEntry.setResetToken(event.getResetToken());
        logEntry.setStatus("PROCESSING");
        return logEntry;
    }
}