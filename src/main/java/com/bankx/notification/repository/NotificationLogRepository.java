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

@ApplicationScoped
public class NotificationLogRepository {

    @Inject
    private MongoClient mongoClient;

    @Inject
    private MongoConfig mongoConfig;

    private MongoCollection<NotificationLog> getCollection() {
        MongoDatabase database = mongoClient.getDatabase(mongoConfig.getDatabaseName());
        return database.getCollection("notification_logs", NotificationLog.class);
    }

    public NotificationLog save(NotificationLog log) {
        try {
            getCollection().insertOne(log);
            return log;
        } catch (MongoWriteException e) {
            // Обработка дубликатов
            if (e.getError() != null && e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
                // Можно выбросить кастомное исключение или вернуть null
                return null;
            }
            throw e;
        }
    }

    public NotificationLog findById(ObjectId id) {
        return getCollection()
                .find(Filters.eq("_id", id))
                .first();
    }

    public List<NotificationLog> findByEmail(String email) {
        return getCollection()
                .find(Filters.eq("email", email))
                .into(new ArrayList<>());
    }

    public List<NotificationLog> findByStatus(String status) {
        return getCollection()
                .find(Filters.eq("status", status))
                .into(new ArrayList<>());
    }

    public void updateStatus(ObjectId id, String status, String errorMessage) {
        Bson update = Updates.combine(
                Updates.set("status", status),
                Updates.set("error_message", errorMessage),
                Updates.inc("attempt_count", 1)
        );
        getCollection().updateOne(Filters.eq("_id", id), update);
    }

    public void markAsSent(ObjectId id) {
        Bson update = Updates.combine(
                Updates.set("status", "SENT"),
                Updates.set("error_message", null),
                Updates.inc("attempt_count", 1)
        );
        getCollection().updateOne(Filters.eq("_id", id), update);
    }

    // (опционально) один раз создадим уникальный индекс по eventId — защита от дублей
    @PostConstruct
    public void ensureIndexes() {
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
    }

    /** true, если запись с таким eventId уже есть (значит, событие обработано) */
    public boolean existsByEventId(String eventId) {
        if (eventId == null) return false;
        return getCollection().find(Filters.eq("eventId", eventId)).first() != null;
    }

    /** фиксируем успешную отправку (дубликаты молча игнорируем) */
    public void saveSuccess(String eventId, String eventType, String target) {
        if (eventId == null) return;

        NotificationLog log = new NotificationLog();
        log.setEventId(eventId);
        log.setEventType(eventType);
        log.setEmail(target);
        log.setStatus("SUCCESS");
        log.setCreatedAt(LocalDateTime.now());

        try {
            getCollection().insertOne(log); // Используем существующий метод
        } catch (MongoWriteException e) {
            if (e.getError() != null && e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
                return;
            }
            throw e;
        }
    }

    // Методы для работы с eventId

    public void saveSimpleEvent(String eventId, String eventType, String target) {
        if (eventId == null) return;

        NotificationLog log = new NotificationLog();
        log.setEventId(eventId);
        log.setEventType(eventType);
        log.setEmail(target); // Используем target как email
        log.setStatus("SUCCESS");

        try {
            getCollection().insertOne(log);
        } catch (MongoWriteException e) {
            // Игнорируем дубликаты
            if (e.getError() != null && e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
                return;
            }
            throw e;
        }
    }

    // Дополнительные методы для работы с eventId

    public NotificationLog findByEventId(String eventId) {
        if (eventId == null) return null;
        return getCollection()
                .find(Filters.eq("event_id", eventId))
                .first();
    }

    public void updateStatusByEventId(String eventId, String status, String errorMessage) {
        Bson update = Updates.combine(
                Updates.set("status", status),
                Updates.set("error_message", errorMessage),
                Updates.inc("attempt_count", 1)
        );
        getCollection().updateOne(Filters.eq("event_id", eventId), update);
    }
}