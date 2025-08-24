package com.bankx.notification.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@ApplicationScoped
public class MongoConfig {

    @Inject
    private ApplicationConfig appConfig;

    private String connectionString;
    private String databaseName;

    @PostConstruct
    public void init() {
        connectionString = appConfig.getProperty("mongodb.connection.string", "mongodb://mongodb:27017/notificationdb");
        databaseName = appConfig.getProperty("mongodb.database", "notificationdb");
    }

    @Produces
    public MongoClient createMongoClient() {
        return MongoClients.create(connectionString);
    }

    public void closeMongoClient(@Disposes MongoClient client) {
        client.close();
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getConnectionString() {
        return connectionString;
    }
}