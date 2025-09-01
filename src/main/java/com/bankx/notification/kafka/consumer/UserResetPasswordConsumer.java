package com.bankx.notification.kafka.consumer;
import com.bankx.notification.config.KafkaConsumerConfig;
import com.bankx.notification.config.KafkaTopicConfig;
import com.bankx.notification.model.dto.UserPasswordResetRequestedEvent;
import com.bankx.notification.service.NotificationService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.errors.WakeupException;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
@ApplicationScoped
public class UserResetPasswordConsumer {
    private static final Logger log = Logger.getLogger(UserResetPasswordConsumer.class.getName());

    @Inject private NotificationService notificationService;
    @Inject private KafkaConsumerConfig kafkaConsumerConfig;
    @Inject private KafkaTopicConfig kafkaTopicConfig;

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private ExecutorService executorService;
    private volatile boolean running = false;
    private KafkaConsumer<String, String> consumer;

    @PostConstruct
    public void init() {
        Properties props = kafkaConsumerConfig.getConsumerProperties(
                "notification-service-reset-group" // ОТДЕЛЬНАЯ groupId
        );
        consumer = new KafkaConsumer<>(props);

        String topic = kafkaTopicConfig.getUserPasswordTopic(); // см. Шаг 4
        consumer.subscribe(Collections.singletonList(topic));

        running = true;
        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(this::poll);
        log.info("Started PasswordResetConsumer for topic: " + topic);
    }

    private void poll() {
        try {
            while (running) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                records.forEach(rec -> {
                    try {
                        UserPasswordResetRequestedEvent e =
                                mapper.readValue(rec.value(), UserPasswordResetRequestedEvent.class);

                        // При двух топиках поле type можно не проверять, но проверка не помешает
                        log.info("Received password reset event: " + e.getEmail());
                        notificationService.processPasswordReset(e);
                        consumer.commitSync();
                    } catch (Exception ex) {
                        log.severe("Error processing reset message: " + ex.getMessage());
                        // не коммитим — ретраится
                    }
                });
            }
        } catch (WakeupException ignored) {
        } catch (Exception e) {
            log.severe("Error in PasswordResetConsumer: " + e.getMessage());
        } finally {
            consumer.close();
        }
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        if (consumer != null) consumer.wakeup();
        if (executorService != null) executorService.shutdown();
        log.info("PasswordResetConsumer stopped");
    }
}
