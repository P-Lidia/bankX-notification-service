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

/**
 * Конфигурационный класс для настройки подключения к MongoDB.
 *
 * <p>Настраивает и предоставляет экземпляр {@link MongoClient} для работы с MongoDB.
 * Поддерживает автоматическую сериализацию/десериализацию POJO объектов.
 */
@ApplicationScoped
public class MongoConfig {
    private static final org.jboss.logging.Logger LOGGER = Logger.getLogger(MongoConfig.class);

    @Inject
    private ApplicationConfig appConfig;

    private String connectionString;
    private String databaseName;

    /**
     * Инициализирует конфигурационные параметры для подключения к MongoDB.
     */
    @PostConstruct
    public void initializeMongoConfiguration() {
        connectionString = appConfig.getProperty("mongodb.connection.string", "mongodb://admin:password@mongodb:27017/bankx-notification?authSource=admin");
        databaseName = appConfig.getProperty("mongodb.database", "bankx-notification");
        LOGGER.infov("MongoDB configuration initialized. Database: {0}", databaseName);
    }

    /**
     * Создает и возвращает экземпляр {@link MongoClient} с поддержкой POJO.
     *
     * <p>Клиент настроен на:
     * <ul>
     *   <li>Использование строки подключения из конфигурации</li>
     *   <li>Автоматическую сериализацию/десериализацию POJO объектов</li>
     *   <li>Стандартное представление UUID</li>
     * </ul>
     *
     * @return настроенный экземпляр {@link MongoClient}
     * @throws RuntimeException если не удалось создать клиент
     */
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

    /**
     * Закрывает экземпляр {@link MongoClient} при уничтожении бина.
     *
     * @param client экземпляр {@link MongoClient} для закрытия
     */
    public void closeMongoClient(@Disposes MongoClient client) {
        try {
            client.close();
            LOGGER.info("MongoClient closed successfully");
        } catch (Exception e) {
            LOGGER.warnv("Error closing MongoClient: {0}", e.getMessage());
        }
    }

    /**
     * Возвращает имя базы данных, используемой приложением.
     *
     * @return имя базы данных
     */
    public String getDatabaseName() {
        return databaseName;
    }

    /**
     * Возвращает строку подключения к MongoDB.
     *
     * @return строка подключения
     */
    public String getConnectionString() {
        return connectionString;
    }
}