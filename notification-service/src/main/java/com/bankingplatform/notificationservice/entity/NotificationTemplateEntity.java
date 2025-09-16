// NotificationTemplateEntity.java
package com.bankingplatform.notificationservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_templates", indexes = {
        @Index(name = "idx_type_channel_lang", columnList = "type, channel, language")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String templateId; // e.g., "FRAUD_ALERT_EMAIL_EN"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationEntity.NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationEntity.NotificationChannel channel;

    @Builder.Default
    private String language = "en";

    @Column(length = 500)
    private String subject;

    @Column(length = 5000)
    private String bodyTemplate;

    @Column(length = 10000)
    private String htmlTemplate;

    @Builder.Default
    private Boolean isActive = true;

    private String description;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}