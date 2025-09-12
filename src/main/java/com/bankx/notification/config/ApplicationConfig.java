package com.bankx.notification.config;

import com.bankx.notification.exception.ApplicationException;
import com.bankx.notification.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Конфигурационный класс для загрузки свойств приложения.
 *
 * <p>Загружает свойства из файла application.properties, расположенного в classpath.
 * Предоставляет методы для получения свойств по ключу с возможностью указания значения по умолчанию.
 */
@ApplicationScoped
public class ApplicationConfig {
    private Properties properties;
    private String appHost;

    /**
     * Инициализирует свойства приложения, загружая их из файла application.properties.
     *
     * @throws ApplicationException если файл свойств не найден или не может быть прочитан
     */
    @PostConstruct
    public void initializeApplicationProperties() {
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                properties.load(input);
                // Загружаем свойство app.host
                this.appHost = properties.getProperty("app.host", "http://localhost:8080");
            } else {
                throw new ApplicationException(
                        ErrorCode.CONFIGURATION_LOAD_ERROR,
                        "Application properties file not found in classpath"
                );
            }
        } catch (IOException e) {
            throw new ApplicationException(
                    ErrorCode.CONFIGURATION_LOAD_ERROR,
                    "Failed to load application properties",
                    e
            );
        }
    }

    public String getAppHost() {
        return appHost;
    }
    /**
     * Возвращает значение свойства по ключу или значение по умолчанию, если свойство не найдено.
     *
     * @param key          ключ свойства
     * @param defaultValue значение по умолчанию
     * @return значение свойства или defaultValue, если свойство не найдено
     */
    public String getProperty(String key, String defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Возвращает значение свойства по ключу или выбрасывает исключение, если свойство не найдено.
     * Используется для обязательных свойств конфигурации.
     *
     * @param key ключ свойства
     * @return значение свойства
     * @throws ApplicationException если свойство не найдено
     */
    public String getProperty(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new ApplicationException(
                    ErrorCode.CONFIGURATION_LOAD_ERROR,
                    "Required property not found: " + key
            );
        }
        return value;
    }
}