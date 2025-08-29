package com.bankx.notification.kafka.consumer;

import com.bankx.notification.config.KafkaConsumerConfig;
import com.bankx.notification.config.KafkaTopicConfig;
import com.bankx.notification.model.dto.UserEvent;
import com.bankx.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

@ApplicationScoped
public class UserRegistrationConsumer {

    @Inject
    private NotificationService notificationService;

    public void testConsume() {
        // Создаем тестовое событие
        UserEvent testEvent = new UserEvent();
        testEvent.setEmail("test@example.com");
        testEvent.setFirstName("Иван");
        testEvent.setLastName("Иванов");
        testEvent.setActivationKey(UUID.randomUUID());

        System.out.println("Создано тестовое событие: " + testEvent);

        // Обрабатываем событие
        notificationService.processUserActivation(testEvent);
    }

    private static final Logger log = Logger.getLogger(UserRegistrationConsumer.class.getName());

    @Inject
    private KafkaConsumerConfig kafkaConsumerConfig;

    @Inject
    private KafkaTopicConfig kafkaTopicConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ExecutorService executorService;
    private volatile boolean running = false;
    private KafkaConsumer<String, String> consumer;

    @PostConstruct
    public void init() {
        Properties consumerProps = kafkaConsumerConfig.getConsumerProperties(
                "notification-service-registration-group"
        );
        consumer = new KafkaConsumer<>(consumerProps);
        String topic = kafkaTopicConfig.getUserRegistrationTopic();
        consumer.subscribe(Collections.singletonList(topic));
        running = true;
        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(this::pollMessages);
        log.info("Started Kafka consumer for topic: " + topic);
    }

    private void pollMessages() {
        try {
            while (running) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

                records.forEach(record -> {
                    try {
                        UserEvent userEvent = objectMapper.readValue(record.value(), UserEvent.class);
                        log.info("Received user registration event: " + userEvent);
                        notificationService.processUserActivation(userEvent);
                    } catch (Exception e) {
                        log.severe("Error processing message: " + e.getMessage());
                    }
                });
            }
        } catch (WakeupException e) {
        } catch (Exception e) {
            log.severe("Error in Kafka consumer: " + e.getMessage());
        } finally {
            consumer.close();
        }
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        if (consumer != null) {
            consumer.wakeup();
        }
        if (executorService != null) {
            executorService.shutdown();
        }
        log.info("Kafka consumer stopped");
    }

}