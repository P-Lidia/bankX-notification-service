package com.bankx.notification.kafka.consumer;

import com.bankx.notification.config.KafkaConsumerConfig;
import com.bankx.notification.config.KafkaTopicConfig;
import com.bankx.notification.model.dto.UserRegistrationEvent;
import com.bankx.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

@Singleton
@Startup
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

    @PostConstruct
    public void init() {
        try {
            System.out.println("=== KAFKA CONSUMER INITIALIZATION STARTED ===");
            System.out.println("Getting consumer properties...");
            Properties consumerProps = kafkaConsumerConfig.getConsumerProperties(
                    "notification-service-registration-group"
            );
            System.out.println("Consumer properties: " + consumerProps);
            String topic = kafkaTopicConfig.getUserRegistrationTopic();
            waitForTopic(topic, consumerProps);
            System.out.println("Creating Kafka consumer instance...");
            consumer = new KafkaConsumer<>(consumerProps);
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
            throw new RuntimeException("Failed to initialize Kafka consumer", e);
        }
    }

    private void waitForTopic(String topic, Properties consumerProps) throws InterruptedException {
        try (AdminClient adminClient = AdminClient.create(consumerProps)) {
            boolean topicExists = false;
            while (!topicExists) {
                ListTopicsResult topics = adminClient.listTopics();
                if (topics.names().get().contains(topic)) {
                    System.out.println("Topic found: " + topic);
                    topicExists = true;
                } else {
                    System.out.println("Topic not created yet, waiting 5 seconds...");
                    Thread.sleep(5000);
                }
            }
        } catch (Exception e) {
            System.err.println("Error checking topic existence: " + e.getMessage());
            throw new RuntimeException(e);
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
                        UserRegistrationEvent userEvent = objectMapper.readValue(record.value(), UserRegistrationEvent.class);
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