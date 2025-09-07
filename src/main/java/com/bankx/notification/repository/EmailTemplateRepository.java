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
    public EmailTemplate findEmailTemplateByType(String templateType) {
        Bson query = Filters.and(
                Filters.eq("templateType", templateType),
                Filters.eq("isActive", true)
        );
        return getEmailTemplatesCollection().find(query).first();
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

    /**
     * Сохраняет новый шаблон письма в базе данных.
     *
     * @param template шаблон письма для сохранения
     * @return сохраненный шаблон
     */
    public EmailTemplate saveEmailTemplate(EmailTemplate template) {
        getEmailTemplatesCollection().insertOne(template);
        return template;
    }

    /**
     * Обновляет существующий шаблон письма.
     *
     * @param template обновленный шаблон письма
     */
    public void updateEmailTemplate(EmailTemplate template) {
        Bson filter = Filters.eq("_id", template.getId());
        getEmailTemplatesCollection().replaceOne(filter, template);
    }

    /**
     * Деактивирует шаблон письма по его типу.
     *
     * @param templateType тип шаблона для деактивации
     */
    public void deactivateEmailTemplate(String templateType) {
        Bson filter = Filters.eq("templateType", templateType);
        Bson update = new org.bson.Document("$set",
                new org.bson.Document("isActive", false)
                        .append("updatedAt", java.time.LocalDateTime.now()));
        getEmailTemplatesCollection().updateOne(filter, update);
    }
}