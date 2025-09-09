package com.bankx.notification.exception;

/**
 * Перечисление кодов ошибок для системы уведомлений.
 *
 * <p>Каждый код ошибки соответствует определенному типу проблем,
 * которые могут возникнуть в работе приложения. Используется для
 * классификации ошибок и их централизованной обработки.
 */
public enum ErrorCode {
    /**
     * Ошибка загрузки конфигурации приложения
     */
    CONFIGURATION_LOAD_ERROR,

    /**
     * Ошибка работы с базой данных
     */
    DATABASE_OPERATION_ERROR,

    /**
     * Ошибка отправки email
     */
    EMAIL_SEND_ERROR,

    /**
     * Шаблон email не найден
     */
    EMAIL_TEMPLATE_NOT_FOUND,

    /**
     * Ошибка работы с Kafka (инициализация, отправка, потребление)
     */
    KAFKA_OPERATION_ERROR,

    /**
     * Обнаружено дублирующееся событие
     */
    DUPLICATE_EVENT_ERROR,

    /**
     * Ошибка десериализации сообщения
     */
    DESERIALIZATION_ERROR,

    /**
     * Неизвестная ошибка
     */
    UNKNOWN_ERROR
}