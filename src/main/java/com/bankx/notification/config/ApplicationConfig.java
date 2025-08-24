package com.bankx.notification.config;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@ApplicationScoped
public class ApplicationConfig {
    private Properties properties;

    @PostConstruct
    public void init() {
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load application properties", e);
        }
    }

    public String getProperty(String key, String defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? value : defaultValue;
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }
}