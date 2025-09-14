package com.bankingplatform.streamprocessor.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    @Autowired(required = false)
    private StreamsBuilderFactoryBean factoryBean;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> health = Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now(),
                "service", "stream-processor"
        );

        return ResponseEntity.ok(health);
    }

    @GetMapping("/kafka-streams")
    public ResponseEntity<Map<String, Object>> getKafkaStreamsHealth() {
        if (factoryBean == null) {
            return ResponseEntity.ok(Map.of(
                    "kafkaStreams", "NOT_CONFIGURED",
                    "timestamp", LocalDateTime.now()
            ));
        }

        KafkaStreams kafkaStreams = factoryBean.getKafkaStreams();
        if (kafkaStreams == null) {
            return ResponseEntity.ok(Map.of(
                    "kafkaStreams", "NOT_STARTED",
                    "timestamp", LocalDateTime.now()
            ));
        }

        Map<String, Object> health = Map.of(
                "kafkaStreams", kafkaStreams.state().toString(),
                "applicationId", kafkaStreams.toString(),
                "timestamp", LocalDateTime.now()
        );

        return ResponseEntity.ok(health);
    }
}