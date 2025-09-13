package com.bankx.notification.repository;

import com.bankx.notification.config.MongoConfig;
import com.bankx.notification.model.entity.NotificationLog;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Updates;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Репозиторий для работы с логами уведомлений в MongoDB.
 *
 * <p>Предоставляет методы для сохранения, поиска и обновления записей о отправленных уведомлениях.
 */
@ApplicationScoped
public class NotificationLogRepository {
    private static final Logger LOG = Logger.getLogger(NotificationLogRepository.class.getName());

    @Inject
    private MongoClient mongoClient;

    @Inject
    private MongoConfig mongoConfig;

    /**
     * Возвращает коллекцию для работы с логами уведомлений.
     *
     * @return коллекция логов уведомлений
     */
    private MongoCollection<NotificationLog> getNotificationLogsCollection() {
        MongoDatabase database = mongoClient.getDatabase(mongoConfig.getDatabaseName());
        return database.getCollection("notification_logs", NotificationLog.class);
    }

    /**
     * Сохраняет запись о уведомлении в базе данных.
     *
     * @param log запись лога для сохранения
     * @return сохраненная запись
     * @throws MongoWriteException если произошла ошибка записи
     */
    public NotificationLog saveNotificationLog(NotificationLog log) {
        try {
            getNotificationLogsCollection().insertOne(log);
            LOG.info("Saved notification log for email: " + log.getEmail());
            return log;
        } catch (MongoWriteException e) {
            LOG.log(Level.SEVERE, "Failed to save notification log: " + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Находит запись лога по идентификатору.
     *
     * @param id идентификатор записи
     * @return запись лога или null, если не найдена
     */
    public NotificationLog findNotificationLogById(ObjectId id) {
        return getNotificationLogsCollection()
                .find(Filters.eq("_id", id))
                .first();
    }

    /**
     * Находит все записи логов для указанного email.
     *
     * @param email адрес электронной почты
     * @return список записей логов
     */
    public List<NotificationLog> findNotificationLogsByEmail(String email) {
        return getNotificationLogsCollection()
                .find(Filters.eq("email", email))
                .into(new ArrayList<>());
    }

    /**
     * Находит все записи логов с указанным статусом.
     *
     * @param status статус уведомления
     * @return список записей логов
     */
    public List<NotificationLog> findNotificationLogsByStatus(String status) {
        return getNotificationLogsCollection()
                .find(Filters.eq("status", status))
                .into(new ArrayList<>());
    }

    /**
     * Обновляет статус записи лога.
     *
     * @param id           идентификатор записи
     * @param status       новый статус
     * @param errorMessage сообщение об ошибке (если есть)
     */
    public void updateNotificationLogStatus(ObjectId id, String status, String errorMessage) {
        Bson update = Updates.combine(
                Updates.set("status", status),
                Updates.set("error_message", errorMessage),
                Updates.inc("attempt_count", 1)
        );
        getNotificationLogsCollection().updateOne(Filters.eq("_id", id), update);
    }

    /**
     * Помечает запись лога как успешно отправленную.
     *
     * @param id идентификатор записи
     */
    public void markNotificationLogAsSent(ObjectId id) {
        Bson update = Updates.combine(
                Updates.set("status", "SENT"),
                Updates.set("error_message", null),
                Updates.inc("attempt_count", 1)
        );
        getNotificationLogsCollection().updateOne(Filters.eq("_id", id), update);
    }

    /**
     * Обновляет статус записи лога по email и activation key.
     *
     * @param email         электронная почта
     * @param activationKey ключ активации
     * @param status        новый статус
     * @param errorMessage  сообщение об ошибке (если есть)
     */
    public void updateNotificationLogStatus(String email, String activationKey, String status, String errorMessage) {
        Bson query = Filters.and(
                Filters.eq("email", email),
                Filters.eq("activation_key", activationKey)
        );

        Bson update = Updates.combine(
                Updates.set("status", status),
                Updates.set("error_message", errorMessage),
                Updates.inc("attempt_count", 1)
        );

        getNotificationLogsCollection().updateOne(query, update);
    }

    /**
     * Обновляет статус записи лога по email и reset token.
     *
     * @param email       электронная почта
     * @param resetToken  токен сброса пароля
     * @param status      новый статус
     * @param errorMessage сообщение об ошибке (если есть)
     */
    public void updateNotificationLogStatusByResetToken(String email, String resetToken, String status, String errorMessage) {
        Bson query = Filters.and(
                Filters.eq("email", email),
                Filters.eq("reset_token", resetToken)
        );

        Bson update = Updates.combine(
                Updates.set("status", status),
                Updates.set("error_message", errorMessage),
                Updates.inc("attempt_count", 1)
        );

        getNotificationLogsCollection().updateOne(query, update);
    }

    /**
     * Создает индексы для коллекции логов уведомлений.
     *
     * <p>Метод выполняется автоматически при инициализации бина.
     * Создает индексы для улучшения производительности запросов.
     */
    @PostConstruct
    public void ensureNotificationLogsIndexes() {
        MongoDatabase database = mongoClient.getDatabase(mongoConfig.getDatabaseName());
        MongoCollection<Document> collection = database.getCollection("notification_logs");

        collection.createIndex(Indexes.ascending("email"));
        collection.createIndex(Indexes.ascending("activation_key"));
        collection.createIndex(Indexes.ascending("reset_token"));
        collection.createIndex(Indexes.ascending("status"));
        collection.createIndex(Indexes.ascending("created_at"));
        collection.createIndex(Indexes.compoundIndex(
                Indexes.ascending("status"),
                Indexes.ascending("created_at")
        ));
    }
}