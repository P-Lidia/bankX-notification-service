package com.bankx.notification.config;

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

    /**
     * Инициализирует свойства приложения, загружая их из файла application.properties.
     */
    @PostConstruct
    public void initializeApplicationProperties() {
        System.out.println("Loading application properties...");
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                properties.load(input);
                System.out.println("Application properties loaded: " + properties);
            } else {
                System.err.println("Application properties file not found!");
            }
        } catch (IOException e) {
            System.err.println("Failed to load application properties: " + e.getMessage());
            e.printStackTrace();
        }
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
     * Возвращает значение свойства по ключу.
     *
     * @param key ключ свойства
     * @return значение свойства или null, если свойство не найдено
     */
    public String getProperty(String key) {
        return properties.getProperty(key);
    }
}