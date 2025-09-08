package com.bankx.notification.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Properties;

/**
 * Конфигурационный класс для настройки потребителей Kafka.
 *
 * <p>Предоставляет методы для создания конфигурационных свойств потребителей Kafka
 * с использованием настроек из application.properties.
 */
@ApplicationScoped
public class KafkaConsumerConfig {

    @Inject
    private ApplicationConfig appConfig;

    /**
     * Создает и возвращает свойства для настройки потребителя Kafka.
     *
     * <p>Настройки включают:
     * <ul>
     *   <li>Адреса брокеров Kafka</li>
     *   <li>Идентификатор группы потребителей</li>
     *   <li>Десериализаторы для ключей и значений (String)</li>
     *   <li>Автоматический сброс смещения на начало (при отсутствии сохраненного смещения)</li>
     *   <li>Автоматическое подтверждение полученных сообщений</li>
     * </ul>
     *
     * @param groupId идентификатор группы потребителей
     * @return свойства для настройки потребителя Kafka
     */
    public Properties getConsumerProperties(String groupId) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                appConfig.getProperty("kafka.bootstrap.servers", "kafka:9092"));
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        return properties;
    }
}