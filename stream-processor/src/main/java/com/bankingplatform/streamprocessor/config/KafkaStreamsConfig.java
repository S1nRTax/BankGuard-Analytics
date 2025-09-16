package com.bankingplatform.streamprocessor.config;

import com.bankingplatform.streamprocessor.model.Transaction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableKafkaStreams
public class KafkaStreamsConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.application.name:stream-processor}")
    private String applicationId;

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kStreamsConfig() {
        Map<String, Object> props = new HashMap<>();

        props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, JsonSerde.class.getName());

        // Force deserialization into our Transaction model
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Transaction.class.getName());
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000);
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 10 * 1024 * 1024L);
        props.put(StreamsConfig.STATE_DIR_CONFIG, "/tmp/kafka-streams/" + applicationId);

        // Handle deserialization errors gracefully
        props.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG,
                org.apache.kafka.streams.errors.LogAndContinueExceptionHandler.class);

        log.info("Kafka Streams config initialized for appId={}", applicationId);
        return new KafkaStreamsConfiguration(props);
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean
    public JsonSerde<Transaction> transactionSerde(ObjectMapper mapper) {
        JsonSerde<Transaction> serde = new JsonSerde<>(Transaction.class, mapper);
        serde.deserializer().ignoreTypeHeaders();  // ignore producer type headers
        serde.deserializer().trustedPackages(
                "com.bankingplatform.transactiongenerator.model," +
                        "com.bankingplatform.streamprocessor.model," +
                        "java.util," +
                        "java.lang"
        );
        return serde;
    }
}
