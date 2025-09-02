package com.bankx.notification.repository;

import com.bankx.notification.config.MongoConfig;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;

@ApplicationScoped
public class NotificationLogRepository {

    @Inject
    private MongoClient mongoClient;

    @Inject
    private MongoConfig mongoConfig;

    private MongoCollection<Document> getCollection() {
        MongoDatabase database = mongoClient.getDatabase(mongoConfig.getDatabaseName());
        return database.getCollection("notification_logs");
    }

    // todo подготовить методы для работы с MongoDB

    // (опционально) один раз создадим уникальный индекс по eventId — защита от дублей
    @PostConstruct
    public void ensureIndexes() {
        getCollection().createIndex(Indexes.ascending("eventId"),
                new IndexOptions().unique(true));
    }

    /** true, если запись с таким eventId уже есть (значит, событие обработано) */
    public boolean existsByEventId(String eventId) {
        if (eventId == null) return false;
        return getCollection().find(Filters.eq("eventId", eventId)).first() != null;
    }

    /** фиксируем успешную отправку (дубликаты молча игнорируем) */
    public void saveSuccess(String eventId, String eventType, String target) {
        if (eventId == null) return;
        Document doc = new Document()
                .append("eventId", eventId)
                .append("eventType", eventType)
                .append("target", target)
                .append("status", "SUCCESS")
                .append("createdAt", System.currentTimeMillis());
        try {
            getCollection().insertOne(doc);
        } catch (MongoWriteException e) {
            // если уже есть запись с таким eventId — просто игнорируем
            if (e.getError() != null && e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
                return;
            }
            throw e;
        }
    }
}