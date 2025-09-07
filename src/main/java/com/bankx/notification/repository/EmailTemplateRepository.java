package com.bankx.notification.repository;

import com.bankx.notification.config.MongoConfig;
import com.bankx.notification.model.entity.EmailTemplate;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.conversions.Bson;

@ApplicationScoped
public class EmailTemplateRepository {

    @Inject
    MongoClient mongoClient;

    @Inject
    MongoConfig mongoConfig;

    private MongoCollection<EmailTemplate> getCollection() {
        MongoDatabase database = mongoClient.getDatabase(mongoConfig.getDatabaseName());
        return database.getCollection("email_templates", EmailTemplate.class);
    }

    public EmailTemplate findByTemplateType(String templateType) {
        Bson query = Filters.and(
                Filters.eq("templateType", templateType),
                Filters.eq("isActive", true)
        );
        return getCollection().find(query).first();
    }
}