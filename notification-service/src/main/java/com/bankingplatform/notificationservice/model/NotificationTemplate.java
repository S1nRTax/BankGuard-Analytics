package com.bankingplatform.notificationservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplate {
    private String templateId;
    private NotificationType type;
    private NotificationChannel channel;
    private String language;
    private String subject;
    private String body;
    private String htmlBody;
}