package com.bankx.notification.repository;

import com.bankx.notification.config.MongoConfig;
import com.bankx.notification.model.entity.NotificationLog;
import com.mongodb.ErrorCategory;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Репозиторий для работы с логами уведомлений в MongoDB.
 *
 * <p>Предоставляет методы для сохранения, поиска и обновления записей о отправленных уведомлениях.
 * Обеспечивает защиту от дубликатов через уникальные индексы и обработку ошибок.
 */
@ApplicationScoped
public class NotificationLogRepository {

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
     * @return сохраненная запись или null в случае дубликата
     * @throws MongoWriteException если произошла ошибка записи (кроме дубликатов)
     */
    public NotificationLog saveNotificationLog(NotificationLog log) {
        try {
            getNotificationLogsCollection().insertOne(log);
            return log;
        } catch (MongoWriteException e) {
            // Обработка дубликатов
            if (e.getError() != null && e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
                return null;
            }
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
     * @param статус статус уведомления
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
     * @param id идентификатор записи
     * @param status новый статус
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
     * Создает индексы для коллекции логов уведомлений.
     *
     * <p>Метод выполняется автоматически при инициализации бина.
     * Создает уникальный индекс по eventId для предотвращения дубликатов
     * и дополнительные индексы для улучшения производительности запросов.
     */
    @PostConstruct
    public void ensureNotificationLogsIndexes() {
        MongoDatabase database = mongoClient.getDatabase(mongoConfig.getDatabaseName());
        MongoCollection<Document> collection = database.getCollection("notification_logs");

        // Уникальный индекс по event_id для защиты от дубликатов
        collection.createIndex(
                Indexes.ascending("event_id"),
                new IndexOptions().unique(true)
        );

        // Другие индексы для улучшения производительности
        collection.createIndex(Indexes.ascending("email"));
        collection.createIndex(Indexes.ascending("status"));
        collection.createIndex(Indexes.ascending("created_at"));
        collection.createIndex(Indexes.ascending("activation_key"));
        collection.createIndex(Indexes.ascending("reset_token"));

        // Индекс для часто используемых комбинаций полей
        collection.createIndex(Indexes.compoundIndex(
                Indexes.ascending("status"),
                Indexes.ascending("created_at")
        ));
    }

    /**
     * Проверяет существование записи с указанным eventId.
     *
     * @param eventId идентификатор события
     * @return true если запись существует, иначе false
     */
    public boolean existsByEventId(String eventId) {
        if (eventId == null) return false;
        return getNotificationLogsCollection().find(Filters.eq("eventId", eventId)).first() != null;
    }

    /**
     * Сохраняет информацию об успешной отправке уведомления.
     * Игнорирует дубликаты событий.
     *
     * @param eventId идентификатор события
     * @param eventType тип события
     * @param target адрес получателя
     */
    public void saveSuccessEvent(String eventId, String eventType, String target) {
        if (eventId == null) return;

        NotificationLog log = new NotificationLog();
        log.setEventId(eventId);
        log.setEventType(eventType);
        log.setEmail(target);
        log.setStatus("SUCCESS");
        log.setCreatedAt(LocalDateTime.now());

        try {
            getNotificationLogsCollection().insertOne(log);
        } catch (MongoWriteException e) {
            if (e.getError() != null && e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
                return;
            }
            throw e;
        }
    }

    /**
     * Сохраняет упрощенную запись о событии.
     * Игнорирует дубликаты событий.
     *
     * @param eventId идентификатор события
     * @param eventType тип события
     * @param target адрес получателя
     */
    public void saveSimpleEvent(String eventId, String eventType, String target) {
        if (eventId == null) return;

        NotificationLog log = new NotificationLog();
        log.setEventId(eventId);
        log.setEventType(eventType);
        log.setEmail(target);
        log.setStatus("SUCCESS");

        try {
            getNotificationLogsCollection().insertOne(log);
        } catch (MongoWriteException e) {
            // Игнорируем дубликаты
            if (e.getError() != null && e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
                return;
            }
            throw e;
        }
    }

    /**
     * Находит запись лога по идентификатору события.
     *
     * @param eventId идентификатор события
     * @return запись лога или null, если не найдена
     */
    public NotificationLog findNotificationLogByEventId(String eventId) {
        if (eventId == null) return null;
        return getNotificationLogsCollection()
                .find(Filters.eq("event_id", eventId))
                .first();
    }

    /**
     * Обновляет статус записи лога по идентификатору события.
     *
     * @param eventId идентификатор события
     * @param status новый статус
     * @param errorMessage сообщение об ошибке (если есть)
     */
    public void updateNotificationLogStatusByEventId(String eventId, String status, String errorMessage) {
        Bson update = Updates.combine(
                Updates.set("status", status),
                Updates.set("error_message", errorMessage),
                Updates.inc("attempt_count", 1)
        );
        getNotificationLogsCollection().updateOne(Filters.eq("event_id", eventId), update);
    }
}