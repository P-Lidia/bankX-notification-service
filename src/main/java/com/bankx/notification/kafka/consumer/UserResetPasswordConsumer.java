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

/**
 * Потребитель Kafka для обработки событий сброса пароля пользователей.
 *
 * <p>Основные функции:
 * <ul>
 *   <li>Подписка на топик сброса пароля пользователей</li>
 *   <li>Обработка сообщений о запросах сброса пароля</li>
 *   <li>Десериализация событий сброса пароля</li>
 *   <li>Передача событий в NotificationService для обработки</li>
 * </ul>
 *
 * <p>Класс работает в singleton-режиме и запускается автоматически при старте приложения.
 */
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

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private ExecutorService executorService;
    private volatile boolean running = false;
    private KafkaConsumer<String, String> consumer;

    /**
     * Инициализирует потребителя Kafka для обработки событий сброса пароля.
     *
     * <p>Метод выполняет следующие действия:
     * <ol>
     *   <li>Получает настройки потребителя из конфигурации</li>
     *   <li>Ожидает создания топика, если он еще не существует</li>
     *   <li>Создает и настраивает экземпляр KafkaConsumer</li>
     *   <li>Подписывается на топик сброса пароля пользователей</li>
     *   <li>Запускает поток для опроса сообщений</li>
     * </ol>
     *
     * @throws RuntimeException если не удалось инициализировать потребителя
     */
    @PostConstruct
    public void initializeConsumer() {
        try {
            Properties consumerProps = kafkaConsumerConfig.getConsumerProperties(
                    "notification-service-reset-group"
            );

            String topic = kafkaTopicConfig.getUserPasswordTopic();
            waitForTopicCreation(topic, consumerProps);

            consumer = new KafkaConsumer<>(consumerProps);
            consumer.subscribe(Collections.singletonList(topic));

            running = true;
            executorService = Executors.newSingleThreadExecutor();
            executorService.execute(this::pollForMessages);

            log.info("Started PasswordResetConsumer for topic: " + topic);
        } catch (Exception e) {
            log.severe("Failed to initialize PasswordResetConsumer: " + e.getMessage());
            throw new RuntimeException("Failed to initialize PasswordResetConsumer", e);
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
            log.severe("Error while waiting for topic: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Опрашивает Kafka на наличие новых сообщений о сбросе пароля.
     *
     * <p>Метод работает в бесконечном цикле, пока running = true.
     * Для каждого полученного сообщения:
     * <ol>
     *   <li>Десериализует сообщение в объект UserResetPasswordEvent</li>
     *   <li>Передает событие в NotificationService для обработки</li>
     *   <li>Логирует успешную обработку или ошибки</li>
     * </ol>
     */
    private void pollForMessages() {
        try {
            while (running) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

                records.forEach(record -> {
                    try {
                        UserResetPasswordEvent resetEvent = objectMapper.readValue(
                                record.value(),
                                UserResetPasswordEvent.class
                        );

                        log.info("Received password reset event: " + resetEvent.getEmail());
                        notificationService.processPasswordReset(resetEvent);
                    } catch (Exception e) {
                        log.severe("Error processing reset message: " + e.getMessage());
                    }
                });
            }
        } catch (WakeupException e) {
            log.info("Consumer woken up for shutdown");
        } catch (Exception e) {
            log.severe("Error in PasswordResetConsumer: " + e.getMessage());
        } finally {
            consumer.close();
            log.info("PasswordResetConsumer closed");
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

        log.info("PasswordResetConsumer stopped");
    }
}