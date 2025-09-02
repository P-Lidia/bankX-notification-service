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

    private static final Logger log = Logger.getLogger(UserRegistrationConsumer.class.getName());

    @Inject
    private KafkaConsumerConfig kafkaConsumerConfig;

    @Inject
    private KafkaTopicConfig kafkaTopicConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ExecutorService executorService;
    private volatile boolean running = false;
    private KafkaConsumer<String, String> consumer;

/*    @PostConstruct
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
    }*/

/*    @PostConstruct
    public void init() {
        try {
            log.info("=== INITIALIZING KAFKA CONSUMER ===");
            log.info("Getting consumer properties...");
            Properties consumerProps = kafkaConsumerConfig.getConsumerProperties(
                    "notification-service-registration-group"
            );
            log.info("Consumer properties: " + consumerProps);

            log.info("Creating Kafka consumer...");
            consumer = new KafkaConsumer<>(consumerProps);

            String topic = kafkaTopicConfig.getUserRegistrationTopic();
            log.info("Subscribing to topic: " + topic);

            consumer.subscribe(Collections.singletonList(topic));
            running = true;

            log.info("Creating executor service...");
            executorService = Executors.newSingleThreadExecutor();

            log.info("Starting message polling thread...");
            executorService.execute(this::pollMessages);

            log.info("=== KAFKA CONSUMER INITIALIZED SUCCESSFULLY ===");
            log.info("Topic: " + topic);
            log.info("Group: notification-service-registration-group");
        } catch (Exception e) {
            log.severe("=== FAILED TO INITIALIZE KAFKA CONSUMER ===");
            log.severe("Error: " + e.getMessage());
            e.printStackTrace();
        }
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
    }*/

    @PostConstruct
    public void init() {
        try {
            System.out.println("=== KAFKA CONSUMER INITIALIZATION STARTED ===");
            System.out.println("Getting consumer properties...");

            Properties consumerProps = kafkaConsumerConfig.getConsumerProperties(
                    "notification-service-registration-group"
            );
            System.out.println("Consumer properties: " + consumerProps);

            System.out.println("Creating Kafka consumer instance...");
            consumer = new KafkaConsumer<>(consumerProps);

            String topic = kafkaTopicConfig.getUserRegistrationTopic();
            System.out.println("Subscribing to topic: " + topic);

            consumer.subscribe(Collections.singletonList(topic));
            running = true;

            System.out.println("Creating executor service...");
            executorService = Executors.newSingleThreadExecutor();

            System.out.println("Starting message polling thread...");
            executorService.execute(this::pollMessages);

            System.out.println("=== KAFKA CONSUMER INITIALIZED SUCCESSFULLY ===");
            System.out.println("Topic: " + topic);
            System.out.println("Group: notification-service-registration-group");
        } catch (Exception e) {
            System.err.println("=== FAILED TO INITIALIZE KAFKA CONSUMER ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void pollMessages() {
        try {
            System.out.println("Starting to poll for messages...");
            while (running) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                System.out.println("Polled " + records.count() + " records");

                records.forEach(record -> {
                    try {
                        System.out.println("Received message: " + record.value());
                        UserEvent userEvent = objectMapper.readValue(record.value(), UserEvent.class);
                        System.out.println("Parsed user registration event: " + userEvent);

                        notificationService.processUserActivation(userEvent);
                        System.out.println("Successfully processed user event for: " + userEvent.getEmail());
                    } catch (Exception e) {
                        System.err.println("Error processing message: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            }
        } catch (WakeupException e) {
            System.out.println("Consumer woken up for shutdown");
        } catch (Exception e) {
            System.err.println("Unexpected error in Kafka consumer: " + e.getMessage());
            e.printStackTrace();
        } finally {
            consumer.close();
            System.out.println("Kafka consumer closed");
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