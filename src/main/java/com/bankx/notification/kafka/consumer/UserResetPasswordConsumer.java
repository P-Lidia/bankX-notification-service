package com.bankx.notification.kafka.consumer;

import com.bankx.notification.config.KafkaConsumerConfig;
import com.bankx.notification.config.KafkaTopicConfig;
import com.bankx.notification.exception.ApplicationException;
import com.bankx.notification.exception.ErrorCode;
import com.bankx.notification.exception.ExceptionMapper;
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
    private static final Logger LOG = Logger.getLogger(UserResetPasswordConsumer.class.getName());

    @Inject
    private NotificationService notificationService;

    @Inject
    private KafkaConsumerConfig kafkaConsumerConfig;

    @Inject
    private KafkaTopicConfig kafkaTopicConfig;

    @Inject
    private ExceptionMapper exceptionMapper;

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
     * @throws ApplicationException если не удалось инициализировать потребителя
     */
    @PostConstruct
    public void initializeConsumer() {
        try {
            LOG.info("=== PASSWORD RESET CONSUMER INITIALIZATION STARTED ===");
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
            LOG.info("=== PASSWORD RESET CONSUMER INITIALIZED SUCCESSFULLY ===");
            LOG.info("Topic: " + topic);
            LOG.info("Group: notification-service-reset-group");
        } catch (Exception e) {
            ApplicationException appEx = new ApplicationException(
                    ErrorCode.KAFKA_OPERATION_ERROR,
                    "Failed to initialize PasswordResetConsumer",
                    e
            );
            exceptionMapper.handleException(appEx);
            throw appEx;
        }
    }

    /**
     * Ожидает создания топика в Kafka.
     *
     * <p>Метод периодически проверяет существование топика до его появления.
     * Это необходим для случаев, когда потребитель запускается раньше топика.
     *
     * @param topic         название топика
     * @param consumerProps свойства потребителя для подключения к Kafka
     * @throws ApplicationException если произошла ошибка при проверке топика
     */
    private void waitForTopicCreation(String topic, Properties consumerProps) {
        try (AdminClient adminClient = AdminClient.create(consumerProps)) {
            boolean topicExists = false;
            int attempt = 0;
            int maxAttempts = 12; // 1 minute total waiting (12 * 5 seconds)
            while (!topicExists && attempt < maxAttempts) {
                attempt++;
                ListTopicsResult topics = adminClient.listTopics();
                if (topics.names().get().contains(topic)) {
                    LOG.info("Topic found: " + topic);
                    topicExists = true;
                } else {
                    LOG.info("Topic not created yet, waiting 5 seconds... (attempt " + attempt + "/" + maxAttempts + ")");
                    Thread.sleep(5000);
                }
            }
            if (!topicExists) {
                throw new ApplicationException(
                        ErrorCode.KAFKA_OPERATION_ERROR,
                        "Topic not found after waiting",
                        "Topic: " + topic
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApplicationException(
                    ErrorCode.KAFKA_OPERATION_ERROR,
                    "Interrupted while waiting for topic",
                    "Topic: " + topic,
                    e
            );
        } catch (Exception e) {
            throw new ApplicationException(
                    ErrorCode.KAFKA_OPERATION_ERROR,
                    "Error while waiting for topic",
                    "Topic: " + topic,
                    e
            );
        }
    }

    /**
     * Опрашивает Kafka на наличие новых сообщений о сбросе пароля.
     *
     * <p>Метод работает в бесконечном цикле, пока running = true.
     * Для каждого полученного сообщения:
     * <ol>
     *   <li>Десериализует сообение в объект UserResetPasswordEvent</li>
     *   <li>Передает событие в NotificationService для обработки</li>
     *   <li>Логирует успешную обработку или ошибки</li>
     * </ol>
     */
    private void pollForMessages() {
        try {
            LOG.info("Starting to poll for password reset messages...");
            while (running) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                LOG.info("Polled " + records.count() + " password reset records");

                records.forEach(record -> {
                    try {
                        UserResetPasswordEvent resetEvent = objectMapper.readValue(
                                record.value(),
                                UserResetPasswordEvent.class
                        );
                        LOG.info("Received password reset event: " + resetEvent.getEmail());
                        notificationService.processPasswordReset(resetEvent);
                        LOG.info("Successfully processed password reset event for: " + resetEvent.getEmail());
                    } catch (ApplicationException e) {
                        // Обрабатываем известные исключения
                        exceptionMapper.handleException(e);
                    } catch (Exception e) {
                        // Оборачиваем неизвестные исключения в ApplicationException
                        ApplicationException appEx = new ApplicationException(
                                ErrorCode.DESERIALIZATION_ERROR,
                                "Error processing password reset message",
                                "Topic: " + kafkaTopicConfig.getUserPasswordTopic(),
                                e
                        );
                        exceptionMapper.handleException(appEx);
                    }
                });
            }
        } catch (WakeupException e) {
            LOG.info("Password reset consumer woken up for shutdown");
        } catch (Exception e) {
            ApplicationException appEx = new ApplicationException(
                    ErrorCode.KAFKA_OPERATION_ERROR,
                    "Unexpected error in password reset consumer",
                    e
            );
            exceptionMapper.handleException(appEx);
        } finally {
            if (consumer != null) {
                consumer.close();
            }
            LOG.info("Password reset consumer closed");
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
        LOG.info("Password reset consumer stopped");
    }
}