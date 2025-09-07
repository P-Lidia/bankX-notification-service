package com.bankx.notification.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

/**
 * Конфигурационный класс для настройки производителей Kafka.
 *
 * <p>Создает и настраивает экземпляр {@link KafkaProducer} для отправки сообщений в Kafka.
 */
@ApplicationScoped
public class KafkaProducerConfig {

    @Inject
    private ApplicationConfig appConfig;

    /**
     * Создает и возвращает настроенного производителя Kafka.
     *
     * <p>Производитель настроен на использование:
     * <ul>
     *   <li>Адресов брокеров из конфигурации приложения</li>
     *   <li>Стринговых сериализаторов для ключей и значений</li>
     * </ul>
     *
     * @return настроенный экземпляр {@link KafkaProducer}
     */
    @Produces
    @ApplicationScoped
    public KafkaProducer<String, String> createKafkaProducer() {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                appConfig.getProperty("kafka.bootstrap.servers", "kafka:9092"));
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new KafkaProducer<>(properties);
    }
}