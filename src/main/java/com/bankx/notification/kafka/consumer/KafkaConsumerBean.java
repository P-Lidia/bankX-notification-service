package com.bankx.notification.kafka.consumer;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;

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
        String bootstrapServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS");
        if (bootstrapServers == null || bootstrapServers.isEmpty()) {
            bootstrapServers = "localhost:9092";
        }
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-service-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList("users.stream", "payments.stream"));
        running = true;
        worker = new Thread(this::pollLoop, "kafka-thread");
        worker.setDaemon(true);
        worker.start();
        log.info("Kafka consumer started (topics: users.stream, payments.stream, bootstrap: " + bootstrapServers + ")");
    }

    private void pollLoop() {
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