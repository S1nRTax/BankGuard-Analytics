package com.bankingplatform.notificationservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private String customerId;
    private NotificationType type;
    private NotificationChannel channel;
    private String subject;
    private String message;
    private Map<String, Object> templateData;
    private NotificationPriority priority;
    private LocalDateTime scheduledTime;
}