package com.bankx.notification.kafka.consumer;

import com.bankx.notification.config.KafkaConsumerConfig;
import com.bankx.notification.config.KafkaTopicConfig;
import com.bankx.notification.model.dto.UserResetPasswordEvent;
import com.bankx.notification.service.NotificationService;
import com.fasterxml.jackson.databind.DeserializationFeature;
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
public class UserResetPasswordConsumer {
    private static final Logger log = Logger.getLogger(UserResetPasswordConsumer.class.getName());
    @Inject
    private NotificationService notificationService;
    @Inject
    private KafkaConsumerConfig kafkaConsumerConfig;
    @Inject
    private KafkaTopicConfig kafkaTopicConfig;
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private ExecutorService executorService;
    private volatile boolean running = false;
    private KafkaConsumer<String, String> consumer;

    @PostConstruct
    public void init() {
        Properties props = kafkaConsumerConfig.getConsumerProperties(
                "notification-service-reset-group"
        );
        String topic = kafkaTopicConfig.getUserPasswordTopic();
        waitForTopic(topic, props);
        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(topic));
        running = true;
        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(this::poll);
        log.info("Started PasswordResetConsumer for topic: " + topic);
    }

    private void waitForTopic(String topic, Properties props) {
        try (AdminClient adminClient = AdminClient.create(props)) {
            boolean topicExists = false;
            while (!topicExists) {
                ListTopicsResult topics = adminClient.listTopics();
                if (topics.names().get().contains(topic)) {
                    log.info("Topic found: " + topic);
                    topicExists = true;
                } else {
                    log.info("Topic not created yet, waiting 5 seconds...");
                    Thread.sleep(5000);
                }
            }
        } catch (Exception e) {
            log.severe("Error while waiting for topic: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void poll() {
        try {
            while (running) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                records.forEach(rec -> {
                    try {
                        UserResetPasswordEvent e =
                                mapper.readValue(rec.value(), UserResetPasswordEvent.class);
                        log.info("Received password reset event: " + e.getEmail());
                        notificationService.processPasswordReset(e);
                    } catch (Exception ex) {
                        log.severe("Error processing reset message: " + ex.getMessage());
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