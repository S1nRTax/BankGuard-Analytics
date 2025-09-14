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
    private String customerId;

    private Long totalTransactions;

    @Column(precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(precision = 15, scale = 2)
    private BigDecimal avgAmount;

    private String mostFrequentMerchantCategory;
    private String preferredLocation;
    private LocalDateTime lastTransactionTime;
    private Double avgRiskScore;

    // Time-window metrics
    private Long transactionsLast1Hour;

    @Column(precision = 15, scale = 2)
    private BigDecimal amountLast1Hour;

    private Long transactionsLast24Hours;

    @Column(precision = 15, scale = 2)
    private BigDecimal amountLast24Hours;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}