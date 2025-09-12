package com.bankingplatform.transactiongenerator.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring..kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, Object> producerFactory(){
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Performance and reliability settings
        configProps.put(ProducerConfig.ACKS_CONFIG, "1"); // wait for the leader broker to confirm
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3); // retry up to 3 times if sending fails
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384); // 16kb batch for efficient network usage
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10); // wait up to 10ms for a batch to fill before sending
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // 32mb buffer

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(){
        return new KafkaTemplate<>(producerFactory());
    }

}
