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

/**
 * Репозиторий для работы с шаблонами электронных писем в MongoDB.
 *
 * <p>Предоставляет методы для поиска и извлечения шаблонов писем,
 * используемых сервисом уведомлений для отправки различных типов писем.
 */
@ApplicationScoped
public class EmailTemplateRepository {

    @Inject
    private MongoClient mongoClient;

    @Inject
    private MongoConfig mongoConfig;

    /**
     * Возвращает коллекцию для работы с шаблонами писем.
     *
     * @return коллекция шаблонов писем
     */
    private MongoCollection<EmailTemplate> getEmailTemplatesCollection() {
        MongoDatabase database = mongoClient.getDatabase(mongoConfig.getDatabaseName());
        return database.getCollection("email_templates", EmailTemplate.class);
    }

    /**
     * Находит шаблон письма по его типу.
     *
     * <p>Возвращает только активные шаблоны (isActive = true).
     *
     * @param templateType тип шаблона (регистрация, сброс пароля и т.д.)
     * @return шаблон письма или null, если не найден
     */
    public EmailTemplate findByTemplateType(String templateType) {
        Bson query = Filters.and(
                Filters.eq("templateType", templateType),
                Filters.eq("isActive", true)
        );
        return getEmailTemplatesCollection().find(query).first();
    }





}