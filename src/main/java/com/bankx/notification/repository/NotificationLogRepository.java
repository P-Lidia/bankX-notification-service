package com.bankx.notification.repository;

import com.bankx.notification.config.MongoConfig;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
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

    // Ваши методы для работы с MongoDB
}