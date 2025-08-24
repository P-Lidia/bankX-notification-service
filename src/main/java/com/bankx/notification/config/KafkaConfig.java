package com.bankx.notification.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

@ApplicationScoped
public class KafkaConfig {

    @Inject
    private ApplicationConfig appConfig;

    @Produces
    @ApplicationScoped
    public KafkaProducer<String, String> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                appConfig.getProperty("kafka.bootstrap.servers", "kafka:9092"));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new KafkaProducer<>(props);
    }

    public String getBootstrapServers() {
        return appConfig.getProperty("kafka.bootstrap.servers", "kafka:9092");
    }

    public String getPaymentTopic() {
        return appConfig.getProperty("kafka.payment.topic", "payment-events");
    }

    public String getUserTopic() {
        return appConfig.getProperty("kafka.user.topic", "user-events");
    }
}