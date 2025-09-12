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

import java.util.logging.Logger;

/**
 * Конфигурационный класс для настройки подключения к MongoDB.
 *
 * <p>Настраивает и предоставляет экземпляр {@link MongoClient} для работы с MongoDB.
 * Поддерживает автоматическую сериализацию/десериализацию POJO объектов.
 * В классе MongoConfig используется PojoCodecProvider, который позволяет MongoDB
 * автоматически сериализовать и десериализовать POJO-объекты. Это означает,
 * что вы можете работать с MongoDB используя свои обычные Java-классы (например, NotificationLog, EmailTemplate),
 * без необходимости преобразовывать их в специальные форматы/
 *
 */
@ApplicationScoped
public class MongoConfig {
    private static final Logger LOG = Logger.getLogger(MongoConfig.class.getName());

    @Inject
    private ApplicationConfig applicationConfig;

    private String connectionString;
    private String databaseName;

    /**
     * Инициализирует конфигурационные параметры для подключения к MongoDB.
     */
    @PostConstruct
    public void initializeMongoConfiguration() {
        connectionString = applicationConfig.getProperty("mongodb.connection.string", "mongodb://appuser:apppassword@mongodb:27017/bankx-notification?authSource=bankx-notification");
        databaseName = applicationConfig.getProperty("mongodb.database", "bankx-notification");
        LOG.info("MongoDB configuration initialized. Database: " + databaseName);
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
            LOG.info("Creating MongoClient with POJO support");
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
            LOG.severe("Failed to create MongoClient: " + e.getMessage());
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
            LOG.info("MongoClient closed successfully");
        } catch (Exception e) {
            LOG.warning("Error closing MongoClient: " + e.getMessage());
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
}