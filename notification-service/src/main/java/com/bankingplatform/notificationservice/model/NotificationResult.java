package com.bankingplatform.notificationservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResult {
    private String notificationId;
    private NotificationStatus status;
    private String message;
    private LocalDateTime sentAt;
    private String externalId; // ID from external service (email provider, SMS gateway)
}