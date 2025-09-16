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
        @Index(name = "idx_customer_id_alert", columnList = "customer_id"),
        @Index(name = "idx_timestamp_alert", columnList = "timestamp"),
        @Index(name = "idx_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAlertEntity {

    @Id
    @Column(name = "alert_id")
    private String alertId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Column(name = "alert_type", nullable = false, length = 100)
    private String alertType;   // <-- new field

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FraudReason reason;

    @Column(length = 1000, nullable = false)
    private String description;

    @Column(nullable = false, length = 20)
    private String severity;    // <-- change Double -> String

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;  // <-- new field (int)

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
