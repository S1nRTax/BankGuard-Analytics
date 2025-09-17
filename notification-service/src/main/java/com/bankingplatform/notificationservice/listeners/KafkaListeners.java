package com.bankingplatform.notificationservice.listeners;

import com.bankingplatform.notificationservice.model.FraudAlert;
import com.bankingplatform.notificationservice.model.NotificationChannel;
import com.bankingplatform.notificationservice.model.NotificationPriority;
import com.bankingplatform.notificationservice.model.NotificationRequest;
import com.bankingplatform.notificationservice.model.NotificationType;
import com.bankingplatform.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaListeners {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "fraud-alerts",
            groupId = "notification-service-fraud-alerts",
            containerFactory = "fraudAlertKafkaListenerContainerFactory"
    )
    public void handleFraudAlert(
            @Payload FraudAlert fraudAlert,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition, // fixed header
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            log.info("Received fraud alert: {} for customer: {} from topic: {}, partition: {}, offset: {}",
                    fraudAlert.getAlertId(), fraudAlert.getCustomerId(), topic, partition, offset);

            NotificationRequest notificationRequest = NotificationRequest.builder()
                    .customerId(fraudAlert.getCustomerId())
                    .type(NotificationType.FRAUD_ALERT)
                    .channel(NotificationChannel.ALL) // Send via all channels
                    .priority(determinePriority(fraudAlert.getSeverity()))
                    .subject("🚨 Fraud Alert - Suspicious Activity Detected")
                    .templateData(createFraudAlertTemplateData(fraudAlert))
                    .build();

            notificationService.sendNotification(notificationRequest);

            acknowledgment.acknowledge();
            log.info("Successfully processed fraud alert: {}", fraudAlert.getAlertId());

        } catch (Exception e) {
            log.error("Error processing fraud alert {}: {}", fraudAlert.getAlertId(), e.getMessage(), e);
        }
    }

    @KafkaListener(
            topics = "customer-notifications",
            groupId = "notification-service-generic",
            containerFactory = "genericKafkaListenerContainerFactory"
    )
    public void handleCustomerNotification(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_KEY) String customerId,
            Acknowledgment acknowledgment) {

        try {
            log.info("Received customer notification for: {}", customerId);

            NotificationRequest notificationRequest = NotificationRequest.builder()
                    .customerId(customerId)
                    .type(NotificationType.SYSTEM_NOTIFICATION)
                    .channel(NotificationChannel.EMAIL)
                    .priority(NotificationPriority.NORMAL)
                    .subject("Account Notification")
                    .message(message)
                    .build();

            notificationService.sendNotification(notificationRequest);
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing customer notification for {}: {}", customerId, e.getMessage(), e);
        }
    }

    @KafkaListener(
            topics = "banking-transactions",
            groupId = "notification-service-transactions",
            containerFactory = "genericKafkaListenerContainerFactory"
    )
    public void handleTransactionNotification(
            @Payload String transactionJson,
            @Header(KafkaHeaders.RECEIVED_KEY) String customerId,
            Acknowledgment acknowledgment) {

        try {
            log.debug("Received transaction notification for customer: {}", customerId);

            NotificationRequest notificationRequest = NotificationRequest.builder()
                    .customerId(customerId)
                    .type(NotificationType.TRANSACTION_CONFIRMATION)
                    .channel(NotificationChannel.EMAIL)
                    .priority(NotificationPriority.NORMAL)
                    .subject("Transaction Confirmation")
                    .templateData(Map.of("transactionData", transactionJson))
                    .build();

            notificationService.sendNotification(notificationRequest);
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing transaction notification for {}: {}", customerId, e.getMessage(), e);
        }
    }

    private NotificationPriority determinePriority(Double severity) {
        if (severity == null) return NotificationPriority.NORMAL;
        if (severity >= 0.9) return NotificationPriority.URGENT;
        if (severity >= 0.7) return NotificationPriority.HIGH;
        if (severity >= 0.4) return NotificationPriority.NORMAL;
        return NotificationPriority.LOW;
    }

    private Map<String, Object> createFraudAlertTemplateData(FraudAlert fraudAlert) {
        return Map.of(
                "alertId", fraudAlert.getAlertId(),
                "customerId", fraudAlert.getCustomerId(),
                "transactionId", fraudAlert.getTransactionId() != null ? fraudAlert.getTransactionId() : "N/A",
                "reason", fraudAlert.getReason().toString(),
                "description", fraudAlert.getDescription(),
                "severity", String.format("%.1f", fraudAlert.getSeverity() * 100) + "%",
                "amount", fraudAlert.getAmount() != null ? fraudAlert.getAmount().toString() : "N/A",
                "timestamp", fraudAlert.getTimestamp().toString(),
                "actionRequired", fraudAlert.getSeverity() > 0.7 ? "IMMEDIATE" : "REVIEW"
        );
    }
}
