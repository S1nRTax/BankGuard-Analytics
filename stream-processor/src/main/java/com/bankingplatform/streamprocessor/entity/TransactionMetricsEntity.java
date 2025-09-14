package com.bankingplatform.streamprocessor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_metrics", indexes = {
        @Index(name = "idx_window_start", columnList = "windowStart")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionMetricsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime windowStart;
    private LocalDateTime windowEnd;

    private Long totalTransactions;

    @Column(precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(precision = 15, scale = 2)
    private BigDecimal avgAmount;

    // Store as JSON strings for simplicity todo: upgrade to @ElementCollection for complex mapping.
    @Column(length = 1000)
    private String transactionsByType; // JSON: {"PAYMENT": 150, "TRANSFER": 30}

    @Column(length = 1000)
    private String transactionsByStatus; // JSON: {"COMPLETED": 160, "FAILED": 20}

    @Column(length = 1000)
    private String transactionsByLocation; // JSON: {"Casablanca": 100, "Rabat": 80}

    private Long alertsGenerated;
    private Double avgRiskScore;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}