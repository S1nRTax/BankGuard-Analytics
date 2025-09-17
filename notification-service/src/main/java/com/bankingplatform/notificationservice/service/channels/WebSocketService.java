package com.bankingplatform.notificationservice.service.channels;

import com.bankingplatform.notificationservice.entity.CustomerContactEntity;
import com.bankingplatform.notificationservice.entity.NotificationEntity;
import com.bankingplatform.notificationservice.model.NotificationResult;
import com.bankingplatform.notificationservice.model.NotificationStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public NotificationResult sendWebSocketNotification(NotificationEntity notification,
                                                        CustomerContactEntity contact) {
        try {
            log.info("Sending WebSocket notification to customer: {} for notification: {}",
                    contact.getCustomerId(), notification.getId());

            // Create WebSocket message payload
            Map<String, Object> payload = Map.of(
                    "notificationId", notification.getId(),
                    "type", notification.getType().toString(),
                    "priority", notification.getPriority().toString(),
                    "subject", notification.getSubject(),
                    "message", notification.getMessage(),
                    "timestamp", LocalDateTime.now().toString()
            );

            // Send to specific customer topic
            String destination = "/topic/notifications/" + contact.getCustomerId();
            messagingTemplate.convertAndSend(destination, payload);

            String externalId = "WS-" + UUID.randomUUID().toString().substring(0, 8);

            log.info("Successfully sent WebSocket notification to customer: {} with ID: {}",
                    contact.getCustomerId(), externalId);

            return NotificationResult.builder()
                    .notificationId(notification.getId())
                    .status(NotificationStatus.SENT)
                    .message("WebSocket notification sent successfully")
                    .sentAt(LocalDateTime.now())
                    .externalId(externalId)
                    .build();

        } catch (Exception e) {
            log.error("Failed to send WebSocket notification to customer {}: {}",
                    contact.getCustomerId(), e.getMessage(), e);

            return NotificationResult.builder()
                    .notificationId(notification.getId())
                    .status(NotificationStatus.FAILED)
                    .message("WebSocket sending failed: " + e.getMessage())
                    .sentAt(LocalDateTime.now())
                    .build();
        }
    }

    public void sendBroadcastNotification(String message, String type) {
        try {
            Map<String, Object> payload = Map.of(
                    "type", type,
                    "message", message,
                    "timestamp", LocalDateTime.now().toString(),
                    "broadcast", true
            );

            messagingTemplate.convertAndSend("/topic/notifications/broadcast", payload);
            log.info("Sent broadcast notification of type: {}", type);

        } catch (Exception e) {
            log.error("Failed to send broadcast notification: {}", e.getMessage(), e);
        }
    }
}