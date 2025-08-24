package com.bankx.notification.repository;

import com.bankx.notification.model.NotificationLog;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Singleton
public class NotificationLogRepository {

    @Inject
    private MongoClient mongoClient;

    private MongoCollection<Document> collection;

    @PostConstruct
    public void init() {
        MongoDatabase database = mongoClient.getDatabase("your_database_name");
        this.collection = database.getCollection("notification_logs");
    }

    public void save(NotificationLog notificationLog) {
        Document doc = new Document()
                .append("clientId", notificationLog.getClientId())
                .append("activationKey", notificationLog.getActivationKey().toString())
                .append("statusId", notificationLog.getStatusId())
                .append("eventType", notificationLog.getEventType())
                .append("email", notificationLog.getEmail())
                .append("phone", notificationLog.getPhone())
                .append("messageContent", notificationLog.getMessageContent())
                .append("sendingChannel", notificationLog.getSendingChannel())
                .append("createdAt", notificationLog.getCreatedAt())
                .append("errorMessage", notificationLog.getErrorMessage());

        collection.insertOne(doc);
    }

    public NotificationLog findById(Long clientId, UUID activationKey, int statusId) {
        Bson filter = Filters.and(
                Filters.eq("clientId", clientId),
                Filters.eq("activationKey", activationKey.toString()),
                Filters.eq("statusId", statusId)
        );

        Document doc = collection.find(filter).first();
        return doc != null ? mapDocumentToNotificationLog(doc) : null;
    }

    public List<NotificationLog> findAll() {
        List<NotificationLog> logs = new ArrayList<>();
        for (Document doc : collection.find()) {
            logs.add(mapDocumentToNotificationLog(doc));
        }
        return logs;
    }

    public List<NotificationLog> findByClientId(Long clientId) {
        List<NotificationLog> logs = new ArrayList<>();
        Bson filter = Filters.eq("clientId", clientId);

        for (Document doc : collection.find(filter)) {
            logs.add(mapDocumentToNotificationLog(doc));
        }
        return logs;
    }

    public void delete(Long clientId, UUID activationKey, int statusId) {
        Bson filter = Filters.and(
                Filters.eq("clientId", clientId),
                Filters.eq("activationKey", activationKey.toString()),
                Filters.eq("statusId", statusId)
        );

        collection.deleteOne(filter);
    }

    private NotificationLog mapDocumentToNotificationLog(Document doc) {
        NotificationLog log = new NotificationLog();
        log.setClientId(doc.getLong("clientId"));
        log.setActivationKey(UUID.fromString(doc.getString("activationKey")));
        log.setStatusId(doc.getInteger("statusId"));
        log.setEventType(doc.getString("eventType"));
        log.setEmail(doc.getString("email"));
        log.setPhone(doc.getString("phone"));
        log.setMessageContent(doc.getString("messageContent"));
        log.setSendingChannel(doc.getString("sendingChannel"));
        log.setCreatedAt(doc.get("createdAt", LocalDateTime.class));
        log.setErrorMessage(doc.getString("errorMessage"));

        return log;
    }
}
