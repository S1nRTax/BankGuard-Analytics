package com.bankingplatform.streamprocessor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_alerts", indexes = {
        @Index(name = "idx_customer_id_alert", columnList = "customerId"),
        @Index(name = "idx_timestamp_alert", columnList = "timestamp"),
        @Index(name = "idx_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAlertEntity {

    @Id
    private String alertId;

    @Column(nullable = false)
    private String customerId;

    private String transactionId;

    @Enumerated(EnumType.STRING)
    private FraudReason reason;

    @Column(length = 1000)
    private String description;

    private Double severity;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AlertStatus status = AlertStatus.NEW;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public enum FraudReason {
        HIGH_AMOUNT, FREQUENT_TRANSACTIONS, UNUSUAL_LOCATION,
        HIGH_RISK_SCORE, VELOCITY_CHECK_FAILED, SUSPICIOUS_PATTERN
    }

    public enum AlertStatus {
        NEW, INVESTIGATING, RESOLVED, FALSE_POSITIVE
    }
}