package com.bankx.notification.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class MongoConfig {

    @Produces
    @ApplicationScoped
    public MongoClient mongoClient() {
        return MongoClients.create("mongodb://localhost:27017");
    }
}