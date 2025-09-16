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
        @Index(name = "idx_window_start", columnList = "window_start")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionMetricsEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private java.util.UUID id;  // match DB if it's UUID

    @Column(name = "metric_date", nullable = false)
    private java.time.LocalDate metricDate;  // required in DB

    @Column(name = "window_start", nullable = false)
    private LocalDateTime windowStart;

    @Column(name = "window_end", nullable = false)
    private LocalDateTime windowEnd;

    @Column(name = "total_transactions", nullable = false)
    private Long totalTransactions;

    @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "avg_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal avgAmount;

    @Column(name = "transactions_by_type", length = 1000, nullable = false)
    private String transactionsByType;

    @Column(name = "transactions_by_status", length = 1000, nullable = false)
    private String transactionsByStatus;

    @Column(name = "transactions_by_location", length = 1000, nullable = false)
    private String transactionsByLocation;

    @Column(name = "alerts_generated", nullable = false)
    private Long alertsGenerated;

    @Column(name = "avg_risk_score", nullable = false)
    private Double avgRiskScore;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
