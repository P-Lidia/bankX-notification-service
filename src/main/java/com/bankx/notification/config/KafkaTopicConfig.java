package com.bankx.notification.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Конфигурационный класс для получения имен топиков Kafka.
 *
 * <p>Предоставляет методы для получения имен топиков, используемых в приложении.
 * Значения топиков могут быть переопределены в файле application.properties.
 */
@ApplicationScoped
public class KafkaTopicConfig {

    @Inject
    private ApplicationConfig appConfig;

    /**
     * Возвращает имя топика для событий транзакций.
     *
     * @return имя топика для транзакций
     */
    public String getTransactionTopic() {
        return appConfig.getProperty("kafka.payment.topic", "notifications.transaction.events");
    }

    /**
     * Возвращает имя топика для событий регистрации пользователей.
     *
     * @return имя топика для регистрации пользователей
     */
    public String getUserRegistrationTopic() {
        return appConfig.getProperty("kafka.user.registration.topic", "notifications.registration.events");
    }

    /**
     * Возвращает имя топика для событий сброса пароля пользователей.
     *
     * @return имя топика для сброса пароля
     */
    public String getUserPasswordTopic() {
        return appConfig.getProperty("kafka.user.reset.password.topic", "notifications.reset.password.events");
    }
}