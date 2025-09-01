package com.bankx.notification.repository;

import com.bankx.notification.config.MongoConfig;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
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
    public boolean existsByEventId(String eventId) {
        if (eventId == null) return false;
        Document doc = getCollection().find(Filters.eq("eventId", eventId)).first();
        return doc != null;
    }

    public void saveSuccess(String eventId, String eventType, String target) {
        if (eventId == null) return;
        Document doc = new Document()
                .append("eventId", eventId)
                .append("eventType", eventType)
                .append("target", target)
                .append("status", "SUCCESS")
                .append("createdAt", System.currentTimeMillis());
        getCollection().insertOne(doc);
    }
}
