package com.bankx.notification.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class KafkaTopicConfig {

    @Inject
    private ApplicationConfig appConfig;

    public String getTransactionTopic() {
        return appConfig.getProperty("kafka.payment.topic", "notifications.transaction.events");
    }

    public String getUserRegistrationTopic() {
        return appConfig.getProperty("kafka.user.topic", "notifications.registration.events");
    }

    public String getUserPasswordTopic() {
        return appConfig.getProperty("kafka.user.topic", "notifications.reset.password.events");
    }
}