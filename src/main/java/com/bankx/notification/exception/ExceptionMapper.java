package com.bankx.notification.exception;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Маппер для обработки и логирования исключений.
 *
 * <p>Централизованно обрабатывает все исключения в приложении,
 * обеспечивая единообразное логирование и обработку ошибок.
 *
 * <p>Для каждого типа ошибки (ErrorCode) определяет appropriate уровень логирования
 * и способ обработки.
 */
@ApplicationScoped
public class ExceptionMapper {
    private static final Logger LOG = LogManager.getLogger(ExceptionMapper.class);

    /**
     * Обрабатывает ApplicationException с учетом его кода ошибки.
     *
     * @param exception исключение для обработки
     */
    public void handleException(ApplicationException exception) {
        Level logLevel = getLogLevelForErrorCode(exception.getErrorCode());
        String logMessage = buildLogMessage(exception);
        LOG.log(logLevel, logMessage, exception);
        //todo Здесь можно добавить дополнительную логику обработки:
        // - отправка уведомлений для критических ошибок
        // - и т.д.
    }

    /**
     * Определяет уровень логирования для кода ошибки.
     *
     * @param errorCode код ошибки
     * @return уровень логирования
     */
    private Level getLogLevelForErrorCode(ErrorCode errorCode) {
        return switch (errorCode) {
            case EMAIL_SEND_ERROR, DESERIALIZATION_ERROR, EMAIL_TEMPLATE_NOT_FOUND -> Level.WARN;
            case DUPLICATE_EVENT_ERROR -> Level.INFO;
            default -> Level.ERROR;
        };
    }

    /**
     * Формирует детальное сообщение для лога на основе исключения.
     *
     * @param exception исключение
     * @return форматированное сообщение для лога
     */
    private String buildLogMessage(ApplicationException exception) {
        StringBuilder message = new StringBuilder();
        message.append("ErrorCode: ").append(exception.getErrorCode());
        message.append(" | Message: ").append(exception.getMessage());
        if (exception.getDetails() != null) {
            message.append(" | Details: ").append(exception.getDetails());
        }
        if (exception.getCause() != null) {
            message.append(" | Cause: ").append(exception.getCause().getMessage());
        }
        return message.toString();
    }
}