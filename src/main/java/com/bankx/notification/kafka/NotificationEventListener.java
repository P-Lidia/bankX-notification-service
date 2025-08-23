package com.bankx.notification.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationEventListener {

    @KafkaListener(topics = "${kafka.topics.user-registered}", groupId = "notification-service-group")
    public void onUserRegistered(@Payload String message) {
        log.info("USER_REGISTERED raw: {}", message);
    }

    @KafkaListener(topics = "${kafka.topics.payment-processed}", groupId = "notification-service-group")
    public void onPaymentProcessed(@Payload String message) {
        log.info("PAYMENT_PROCESSED raw: {}", message);
    }
}