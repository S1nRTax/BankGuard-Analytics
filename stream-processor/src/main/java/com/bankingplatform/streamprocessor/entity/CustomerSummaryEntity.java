package com.bankingplatform.streamprocessor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_summaries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSummaryEntity {

    @Id
    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "total_transactions", nullable = false)
    private Long totalTransactions = 0L;

    @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "avg_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal avgAmount = BigDecimal.ZERO;

    @Column(name = "most_frequent_merchant_category")
    private String mostFrequentMerchantCategory;

    @Column(name = "preferred_location")
    private String preferredLocation;

    @Column(name = "last_transaction_time")
    private LocalDateTime lastTransactionTime;

    @Column(name = "avg_risk_score")
    private Double avgRiskScore;

    // Time-window metrics
    @Column(name = "transactions_last_1_hour", nullable = false)
    private Long transactionsLast1Hour = 0L;

    @Column(name = "amount_last_1_hour", precision = 15, scale = 2, nullable = false)
    private BigDecimal amountLast1Hour = BigDecimal.ZERO;

    @Column(name = "transactions_last_24_hours", nullable = false)
    private Long transactionsLast24Hours = 0L;

    @Column(name = "amount_last_24_hours", precision = 15, scale = 2, nullable = false)
    private BigDecimal amountLast24Hours = BigDecimal.ZERO;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
