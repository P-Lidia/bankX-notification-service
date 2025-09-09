package com.bankx.notification.exception;

/**
 * Кастомное исключение для приложения BankX Notification Service.
 *
 * <p>Содержит дополнительную информацию об ошибке:
 * <ul>
 *   <li>Код ошибки для классификации</li>
 *   <li>Детали ошибки для отладки</li>
 *   <li>Исходное исключение (cause)</li>
 * </ul>
 *
 * <p>Используется для единообразной обработки ошибок во всем приложении.
 */
public class ApplicationException extends RuntimeException {
    private final ErrorCode errorCode;
    private final String details;

    /**
     * Создает новое исключение с указанным кодом и сообщением.
     *
     * @param errorCode код ошибки
     * @param message   понятное сообщение об ошибке
     */
    public ApplicationException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = null;
    }

    /**
     * Создает новое исключение с указанным кодом, сообщением и деталями.
     *
     * @param errorCode код ошибки
     * @param message   понятное сообщение об ошибке
     * @param details   дополнительные детали для отладки
     */
    public ApplicationException(ErrorCode errorCode, String message, String details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    /**
     * Создает новое исключение с указанным кодом, сообщением и причиной.
     *
     * @param errorCode код ошибки
     * @param message   понятное сообщение об ошибке
     * @param cause     исходное исключение
     */
    public ApplicationException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = null;
    }

    /**
     * Создает новое исключение с указанным кодом, сообщением, деталями и причиной.
     *
     * @param errorCode код ошибки
     * @param message   понятное сообщение об ошибке
     * @param details   дополнительные детали для отладки
     * @param cause     исходное исключение
     */
    public ApplicationException(ErrorCode errorCode, String message, String details, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = details;
    }

    /**
     * Возвращает код ошибки.
     *
     * @return код ошибки
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Возвращает детали ошибки.
     *
     * @return детали ошибки или null, если не указаны
     */
    public String getDetails() {
        return details;
    }

    @Override
    public String toString() {
        return "ApplicationException{" +
                "errorCode=" + errorCode +
                ", message='" + getMessage() + '\'' +
                ", details='" + details + '\'' +
                ", cause=" + getCause() +
                '}';
    }
}