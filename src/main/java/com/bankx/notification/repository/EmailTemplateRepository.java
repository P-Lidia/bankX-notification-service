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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Репозиторий для работы с шаблонами электронных писем в MongoDB.
 *
 * <p>Предоставляет методы для поиска и извлечения шаблонов писем,
 * используемых сервисом уведомлений для отправки различных типов писем.
 */
@ApplicationScoped
public class EmailTemplateRepository {
    private static final Logger LOG = Logger.getLogger(EmailTemplateRepository.class.getName());

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
        LOG.fine("Getting 'email_templates' collection from database: " + mongoConfig.getDatabaseName());
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
        LOG.info("Searching for active EmailTemplate by templateType: " + templateType);
        try {
            Bson query = Filters.and(
                    Filters.eq("templateType", templateType),
                    Filters.eq("isActive", true)
            );
            EmailTemplate template = getEmailTemplatesCollection().find(query).first();
            if (template != null) {
                LOG.fine("Found EmailTemplate for templateType: " + templateType);
            } else {
                LOG.warning("No active EmailTemplate found for templateType: " + templateType);
            }
            return template;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error occurred while finding EmailTemplate for templateType: " + templateType, e);
            return null;
        }
    }
}