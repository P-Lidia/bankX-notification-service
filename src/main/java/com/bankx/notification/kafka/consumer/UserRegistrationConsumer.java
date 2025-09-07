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

/**
 * Потребитель Kafka для обработки событий регистрации пользователей.
 *
 * <p>Основные функции:
 * <ul>
 *   <li>Подписка на топик регистрации пользователей</li>
 *   <li>Обработка сообщений о регистрации пользователей</li>
 *   <li>Десериализация событий регистрации</li>
 *   <li>Передача событий в NotificationService для обработки</li>
 * </ul>
 *
 * <p>Класс работает в singleton-режиме и запускается автоматически при старте приложения.
 */
@Singleton
@Startup
public class UserRegistrationConsumer {
    private static final Logger log = Logger.getLogger(UserRegistrationConsumer.class.getName());

    @Inject
    private NotificationService notificationService;

    @Inject
    private KafkaConsumerConfig kafkaConsumerConfig;

    @Inject
    private KafkaTopicConfig kafkaTopicConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ExecutorService executorService;
    private volatile boolean running = false;
    private KafkaConsumer<String, String> consumer;

    /**
     * Инициализирует потребителя Kafka.
     *
     * <p>Метод выполняет следующие действия:
     * <ol>
     *   <li>Получает настройки потребителя из конфигурации</li>
     *   <li>Ожидает создания топика, если он еще не существует</li>
     *   <li>Создает и настраивает экземпляр KafkaConsumer</li>
     *   <li>Подписывается на топик регистрации пользователей</li>
     *   <li>Запускает поток для опроса сообщений</li>
     * </ol>
     *
     * @throws RuntimeException если не удалось инициализировать потребителя
     */
    @PostConstruct
    public void initializeConsumer() {
        try {
            log.info("=== KAFKA CONSUMER INITIALIZATION STARTED ===");
            log.info("Getting consumer properties...");

            Properties consumerProps = kafkaConsumerConfig.getConsumerProperties(
                    "notification-service-registration-group"
            );

            String topic = kafkaTopicConfig.getUserRegistrationTopic();
            waitForTopicCreation(topic, consumerProps);

            log.info("Creating Kafka consumer instance...");
            consumer = new KafkaConsumer<>(consumerProps);

            log.info("Subscribing to topic: " + topic);
            consumer.subscribe(Collections.singletonList(topic));

            running = true;
            executorService = Executors.newSingleThreadExecutor();
            executorService.execute(this::pollForMessages);

            log.info("=== KAFKA CONSUMER INITIALIZED SUCCESSFULLY ===");
            log.info("Topic: " + topic);
            log.info("Group: notification-service-registration-group");
        } catch (Exception e) {
            log.severe("=== FAILED TO INITIALIZE KAFKA CONSUMER ===");
            log.severe("Error: " + e.getMessage());
            throw new RuntimeException("Failed to initialize Kafka consumer", e);
        }
    }

    /**
     * Ожидает создания топика в Kafka.
     *
     * <p>Метод периодически проверяет существование топика до его появления.
     * Это необходимо для случаев, когда потребитель запускается раньше топика.
     *
     * @param topic название топика
     * @param consumerProps свойства потребителя для подключения к Kafka
     * @throws InterruptedException если поток был прерван во время ожидания
     * @throws RuntimeException если произошла ошибка при проверке топика
     */
    private void waitForTopicCreation(String topic, Properties consumerProps) throws InterruptedException {
        try (AdminClient adminClient = AdminClient.create(consumerProps)) {
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
            log.severe("Error checking topic existence: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Опрашивает Kafka на наличие новых сообщений.
     *
     * <p>Метод работает в бесконечном цикле, пока running = true.
     * Для каждого полученного сообщения:
     * <ol>
     *   <li>Десериализует сообщение в объект UserRegistrationEvent</li>
     *   <li>Передает событие в NotificationService для обработки</li>
     *   <li>Логирует успешную обработку или ошибки</li>
     * </ol>
     */
    private void pollForMessages() {
        try {
            log.info("Starting to poll for messages...");

            while (running) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                log.info("Polled " + records.count() + " records");

                records.forEach(record -> {
                    try {
                        log.info("Received message: " + record.value());
                        UserRegistrationEvent userEvent = objectMapper.readValue(
                                record.value(),
                                UserRegistrationEvent.class
                        );

                        log.info("Parsed user registration event: " + userEvent);
                        notificationService.processUserActivation(userEvent);

                        log.info("Successfully processed user event for: " + userEvent.getEmail());
                    } catch (Exception e) {
                        log.severe("Error processing message: " + e.getMessage());
                    }
                });
            }
        } catch (WakeupException e) {
            log.info("Consumer woken up for shutdown");
        } catch (Exception e) {
            log.severe("Unexpected error in Kafka consumer: " + e.getMessage());
        } finally {
            consumer.close();
            log.info("Kafka consumer closed");
        }
    }

    /**
     * Останавливает потребителя Kafka.
     *
     * <p>Метод выполняет graceful shutdown:
     * <ol>
     *   <li>Устанавливает флаг running = false</li>
     *   <li>Пробуждает потребителя, если он заблокирован в poll</li>
     *   <li>Останавливает исполнительный сервис</li>
     * </ol>
     */
    @PreDestroy
    public void shutdownConsumer() {
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