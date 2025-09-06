package com.bankx.notification.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MongoConfig {
    private static final org.jboss.logging.Logger LOGGER = Logger.getLogger(MongoConfig.class);
    @Inject
    private ApplicationConfig appConfig;
    private String connectionString;
    private String databaseName;

    @PostConstruct
    public void init() {
        connectionString = appConfig.getProperty("mongodb.connection.string", "mongodb://admin:password@mongodb:27017/bankx-notification?authSource=admin");
        databaseName = appConfig.getProperty("mongodb.database", "bankx-notification");
        LOGGER.infov("MongoDB configuration initialized. Database: {0}", databaseName);
    }

    @Produces
    @ApplicationScoped
    public MongoClient createMongoClient() {
        try {
            LOGGER.info("Creating MongoClient with POJO support");
            CodecRegistry pojoCodecRegistry = CodecRegistries.fromRegistries(
                    MongoClientSettings.getDefaultCodecRegistry(),
                    CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build())
            );
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(connectionString))
                    .codecRegistry(pojoCodecRegistry)
                    .uuidRepresentation(UuidRepresentation.STANDARD)
                    .build();
            return MongoClients.create(settings);
        } catch (Exception e) {
            LOGGER.error("Failed to create MongoClient: " + e.getMessage());
            throw new RuntimeException("Failed to create MongoClient", e);
        }
    }

    public void closeMongoClient(@Disposes MongoClient client) {
        try {
            client.close();
            LOGGER.info("MongoClient closed successfully");
        } catch (Exception e) {
            LOGGER.warnv("Error closing MongoClient: {0}", e.getMessage());
        }
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getConnectionString() {
        return connectionString;
    }
}