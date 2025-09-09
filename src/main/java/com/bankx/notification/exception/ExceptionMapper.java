package com.bankx.notification.exception;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;

import jakarta.enterprise.context.ApplicationScoped;

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
        // Определяем уровень логирования в зависимости от типа ошибки
        Level logLevel = getLogLevelForErrorCode(exception.getErrorCode());

        // Формируем детальное сообщение для лога
        String logMessage = buildLogMessage(exception);

        // Логируем с соответствующим уровнем
        LOG.log(logLevel, logMessage, exception);

        //todo Здесь можно добавить дополнительную логику обработки:
        // - отправка уведомлений для критических ошибок
        // - и т.д.
    }

    /**
     * Обрабатывает любое исключение, преобразуя его при необходимости в ApplicationException.
     *
     * @param throwable исключение для обработки
     */
    public void handleException(Throwable throwable) {
        if (throwable instanceof ApplicationException) {
            handleException((ApplicationException) throwable);
        } else {
            // Преобразуем неизвестное исключение в ApplicationException
            ApplicationException appException = new ApplicationException(
                    ErrorCode.UNKNOWN_ERROR,
                    "Unexpected error occurred",
                    throwable.getMessage(),
                    throwable
            );
            handleException(appException);
        }
    }

    /**
     * Определяет уровень логирования для кода ошибки.
     *
     * @param errorCode код ошибки
     * @return уровень логирования
     */
    private Level getLogLevelForErrorCode(ErrorCode errorCode) {
        switch (errorCode) {
            case CONFIGURATION_LOAD_ERROR:
            case DATABASE_OPERATION_ERROR:
            case KAFKA_OPERATION_ERROR:
                return Level.ERROR;

            case EMAIL_SEND_ERROR:
            case DESERIALIZATION_ERROR:
                return Level.WARN;

            case DUPLICATE_EVENT_ERROR:
                return Level.INFO;

            case EMAIL_TEMPLATE_NOT_FOUND:
                return Level.WARN;

            case UNKNOWN_ERROR:
            default:
                return Level.ERROR;
        }
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