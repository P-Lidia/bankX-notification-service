package com.bankx.notification.config;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@ApplicationScoped
public class ApplicationConfig {
    private Properties properties;

/*    @PostConstruct
    public void init() {
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load application properties", e);
        }
    }*/
@PostConstruct
public void init() {
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

    public String getProperty(String key, String defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? value : defaultValue;
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }
}