package com.bankingplatform.streamprocessor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_customer_id", columnList = "customerId"),
        @Index(name = "idx_timestamp", columnList = "timestamp"),
        @Index(name = "idx_merchant_category", columnList = "merchantCategory")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEntity {

    @Id
    private String transactionId;

    @Column(nullable = false)
    private String customerId;

    private String accountNumber;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    private String currency;
    private String merchantName;
    private String merchantCategory;
    private String description;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    private String sourceLocation;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    private String ipAddress;
    private String deviceId;
    private Boolean isInternational;
    private Double riskScore;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public enum TransactionType {
        PAYMENT, TRANSFER, WITHDRAWAL, DEPOSIT, REFUND, SUBSCRIPTION, INVESTMENT
    }

    public enum TransactionStatus {
        PENDING, COMPLETED, FAILED, CANCELLED, PROCESSING
    }
}