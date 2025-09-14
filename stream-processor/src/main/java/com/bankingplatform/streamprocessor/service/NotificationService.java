package com.bankingplatform.streamprocessor.service;

import com.bankingplatform.streamprocessor.entity.FraudAlertEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendFraudAlert(FraudAlertEntity alert) {
        try {
            // Send to notification service via Kafka
            kafkaTemplate.send("fraud-alerts", alert.getCustomerId(), alert)
                    .whenComplete((result, failure) -> {
                        if (failure != null) {
                            log.error("Failed to send fraud alert notification: {}", failure.getMessage());
                        } else {
                            log.info("Sent fraud alert notification for customer: {}", alert.getCustomerId());
                        }
                    });
        } catch (Exception e) {
            log.error("Error sending fraud alert notification: {}", e.getMessage(), e);
        }
    }

    public void sendCustomerAlert(String customerId, String message) {
        try {
            kafkaTemplate.send("customer-notifications", customerId, message);
            log.info("Sent customer notification to: {}", customerId);
        } catch (Exception e) {
            log.error("Error sending customer notification: {}", e.getMessage(), e);
        }
    }
}