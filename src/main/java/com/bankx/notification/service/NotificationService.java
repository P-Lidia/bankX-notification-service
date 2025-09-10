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
 *   <li>Предотвращение обработки дубликатов событий через механизм eventId</li>
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
     *   <li>Проверяет, не обрабатывалось ли уже событие с данным eventId</li>
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
                    .collect (Collectors.joining("; "));
            LOG.warning("Validation failed for UserRegistrationEvent: " + msg);
            throw new ApplicationException (ErrorCode.VALIDATION_ERROR,
                    "Validation failed", msg);
        }
        // Проверяем дубликат события
        if (userEvent.getEventId() != null && notificationLogRepository.existsByEventId(userEvent.getEventId())) {
            throw new ApplicationException(
                    ErrorCode.DUPLICATE_EVENT_ERROR,
                    "Duplicate activation event detected",
                    "Event ID: " + userEvent.getEventId()
            );
        }
        try {
            NotificationLog logEntry = createActivationLogEntry(userEvent);
            notificationLogRepository.saveNotificationLog(logEntry);
            String activationLink = applicationConfig.getAppHost() + "/activate?key=" + userEvent.getActivationKey();
            emailService.sendActivationEmail(
                    userEvent.getEmail(),
                    activationLink,
                    userEvent.getFirstName(),
                    userEvent.getLastName()
            );
            updateLogStatusAsSent(logEntry, userEvent.getEventId());
            LOG.info("Activation email sent to: " + userEvent.getEmail());
        } catch (ApplicationException e) {
            // Пробрасываем ApplicationException как есть
            handleActivationError(userEvent, e);
            throw e;
        } catch (Exception e) {
            // Оборачиваем другие исключения в ApplicationException
            ApplicationException appEx = new ApplicationException(
                    ErrorCode.EMAIL_SEND_ERROR,
                    "Failed to process user activation",
                    "User: " + userEvent.getEmail(),
                    e
            );
            handleActivationError(userEvent, appEx);
            throw appEx;
        }
    }

    /**
     * Обрабатывает событие сброса пароля пользователя.
     *
     * <p>Метод выполняет следующие действия:
     * <ol>
     *   <li>Валидирует входной объект по аннотациям из ДТО</li>
     *   <li>Проверяет, не обрабатывалось ли уже событие с данным eventId</li>
     *   <li>Создает запись в логе уведомлений со статусом PROCESSING</li>
     *   <li>Отправляет письмо сброса пароля через EmailService</li>
     *   <li>Обновляет статус записи в логе на SENT при успешной отправке</li>
     *   <li>Сохраняет информацию об успешной обработке события</li>
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
            throw new ApplicationException (ErrorCode.VALIDATION_ERROR,
                    "Validation failed", msg);
        }
        // Проверяем дубликат события
        if (event.getEventId() != null && notificationLogRepository.existsByEventId(event.getEventId())) {
            throw new ApplicationException(
                    ErrorCode.DUPLICATE_EVENT_ERROR,
                    "Duplicate password reset event detected",
                    "Event ID: " + event.getEventId()
            );
        }
        try {
            NotificationLog logEntry = createPasswordResetLogEntry(event);
            notificationLogRepository.saveNotificationLog(logEntry);
            String resetLink = applicationConfig.getAppHost() + "/recover?key=" + event.getResetToken();
            emailService.sendPasswordResetEmail(
                    event.getEmail(),
                    resetLink,
                    event.getFirstName(),
                    event.getLastName()
            );
            updateLogStatusAsSent(logEntry, event.getEventId());
            LOG.info("Password reset email sent to: " + event.getEmail());
        } catch (ApplicationException e) {
            // Пробрасываем ApplicationException как есть
            handlePasswordResetError(event, e);
            throw e;
        } catch (Exception e) {
            // Оборачиваем другие исключения в ApplicationException
            ApplicationException appEx = new ApplicationException(
                    ErrorCode.EMAIL_SEND_ERROR,
                    "Failed to process password reset",
                    "User: " + event.getEmail(),
                    e
            );
            handlePasswordResetError(event, appEx);
            throw appEx;
        }
    }
//todo: раскомментировать как появятся данные сценарии, шаблоны в БД уже заведены
/*
    */
/**
     * Отправляет уведомление об успешной активации аккаунта.
     *
     * <p>Используется после подтверждения активации аккаунта пользователем
     * для отправки подтверждающего уведомления.
     *
     * @param email     электронная почта пользователя
     * @param firstName имя пользователя
     * @param lastName  фамилия пользователя
     * @throws ApplicationException если не удалось отправить уведомление
     *//*

    public void notifyAccountActivated(String email, String firstName, String lastName) {
        try {
            emailService.sendAccountActivatedEmail(email, firstName, lastName);
            LOG.info("Account activated notification sent to: " + email);
        } catch (Exception e) {
            throw new ApplicationException(
                    ErrorCode.EMAIL_SEND_ERROR,
                    "Failed to send account activated notification",
                    "User: " + email,
                    e
            );
        }
    }

    */
/**
     * Отправляет уведомление об успешном сбросе пароля.
     *
     * <p>Используется после успешного изменения пароля пользователем
     * для отправки подтверждающего уведомления.
     *
     * @param email     электронная почта пользователя
     * @param firstName имя пользователя
     * @param lastName  фамилия пользователя
     * @throws ApplicationException если не удалось отправить уведомление
     *//*

    public void notifyPasswordResetSuccess(String email, String firstName, String lastName) {
        try {
            emailService.sendPasswordResetSuccessEmail(email, firstName, lastName);
            LOG.info("Password reset success notification sent to: " + email);
        } catch (Exception e) {
            throw new ApplicationException(
                    ErrorCode.EMAIL_SEND_ERROR,
                    "Failed to send password reset success notification",
                    "User: " + email,
                    e
            );
        }
    }
*/

    // Вспомогательные методы

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
            notificationLogRepository.markNotificationLogAsSent(logEntry.getId());
        } else {
            notificationLogRepository.updateNotificationLogStatusByEventId(eventId, "SENT", null);
        }
    }

    /**
     * Обрабатывает ошибку при активации пользователя.
     *
     * @param userEvent событие регистрации пользователя
     * @param e         исключение, вызвавшее ошибку
     */
    private void handleActivationError(UserRegistrationEvent userEvent, Exception e) {
        // Обрабатываем исключение через ExceptionMapper
        if (e instanceof ApplicationException) {
            exceptionMapper.handleException((ApplicationException) e);
        } else {
            exceptionMapper.handleException(e);
        }
        // Обновляем статус в логе
        if (userEvent.getEventId() != null) {
            notificationLogRepository.updateNotificationLogStatusByEventId(
                    userEvent.getEventId(),
                    "FAILED",
                    e.getMessage()
            );
        }
    }

    /**
     * Обрабатывает ошибку при сбросе пароля.
     *
     * @param event событие сброса пароля
     * @param e     исключение, вызвавшее ошибку
     */
    private void handlePasswordResetError(UserResetPasswordEvent event, Exception e) {
        // Обрабатываем исключение через ExceptionMapper
        if (e instanceof ApplicationException) {
            exceptionMapper.handleException((ApplicationException) e);
        } else {
            exceptionMapper.handleException(e);
        }
        // Обновляем статус в логе
        if (event.getEventId() != null) {
            notificationLogRepository.updateNotificationLogStatusByEventId(
                    event.getEventId(),
                    "FAILED",
                    e.getMessage()
            );
        }
    }
}