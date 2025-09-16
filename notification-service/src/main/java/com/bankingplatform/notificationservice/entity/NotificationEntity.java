// NotificationEntity.java
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
@Table(name = "notifications", indexes = {
        @Index(name = "idx_customer_id", columnList = "customerId"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_created_at", columnList = "createdAt"),
        @Index(name = "idx_type", columnList = "type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Column(length = 500)
    private String subject;

    @Column(length = 2000)
    private String message;

    @Column(length = 5000)
    private String htmlMessage;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private NotificationPriority priority = NotificationPriority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    private String recipient; // email, phone, etc.
    private String externalId; // ID from external service
    private String errorMessage;
    private Integer retryCount;

    private LocalDateTime scheduledTime;
    private LocalDateTime sentAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Related entities
    private String sourceAlertId; // For fraud alerts
    private String transactionId;

    public enum NotificationType {
        FRAUD_ALERT, TRANSACTION_CONFIRMATION, ACCOUNT_LOCKED,
        DAILY_SUMMARY, MARKETING, SYSTEM_NOTIFICATION, PAYMENT_REMINDER
    }

    public enum NotificationChannel {
        EMAIL, SMS, PUSH, WEBSOCKET, ALL
    }

    public enum NotificationPriority {
        LOW, NORMAL, HIGH, URGENT
    }

    public enum NotificationStatus {
        PENDING, SENT, DELIVERED, FAILED, RETRYING, CANCELLED
    }
}
