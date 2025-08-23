package com.bankx.notification.kafka;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Logger;

@Singleton
@Startup
public class KafkaConsumerBean {
    private static final Logger log = Logger.getLogger(KafkaConsumerBean.class.getName());
    private KafkaConsumer<String, String> consumer;
    private Thread worker;
    private volatile boolean running = false;

    @PostConstruct
    public void init() {
        // Минимальные настройки Kafka consumer
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-service-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // Создаём консюмер и подписываемся на ДВА топика
        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList("users.stream", "payments.stream"));

        // Запускаем простой цикл чтения в отдельном потоке
        running = true;
        worker = new Thread(this::pollLoop, "kafka-thread");
        worker.setDaemon(true);
        worker.start();
        log.info("Kafka consumer started (topics: users.stream, payments.stream)");
    }

    private void pollLoop() {
        while (running) {
            consumer.poll(Duration.ofSeconds(1)).forEach(record ->
                    log.info("Received from [" + record.topic() + "]: " + record.value())
            );
        }
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        if (consumer != null) {
            try { consumer.close(); } catch (Exception ignore) {}
        }
        log.info("Kafka consumer stopped");
    }
}