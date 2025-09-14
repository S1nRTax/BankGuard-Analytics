package com.bankingplatform.streamprocessor.service;

import com.bankingplatform.streamprocessor.entity.TransactionEntity;
import com.bankingplatform.streamprocessor.model.Transaction;
import com.bankingplatform.streamprocessor.repository.TransactionRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionProcessingService {

    private final TransactionRepository transactionRepository;
    private final CustomerSummaryService customerSummaryService;
    private final FraudDetectionService fraudDetectionService;
    private final MetricsAggregationService metricsService;
    private final MeterRegistry meterRegistry;

    // Metrics
    private Counter processedTransactionsCounter;
    private Counter fraudAlertsCounter;
    private Timer processingTimer;

    @PostConstruct
    public void initMetrics() {
        this.processedTransactionsCounter = Counter.builder("transactions_processed_total")
                .description("Total number of transactions processed")
                .register(meterRegistry);

        this.fraudAlertsCounter = Counter.builder("fraud_alerts_generated_total")
                .description("Total number of fraud alerts generated")
                .register(meterRegistry);

        this.processingTimer = Timer.builder("transaction_processing_duration")
                .description("Time taken to process a transaction")
                .register(meterRegistry);
    }

    @Transactional
    public void processTransaction(Transaction transaction) {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            log.debug("Starting processing for transaction: {}", transaction.getTransactionId());

            // 1. Store the transaction
            TransactionEntity savedTransaction = storeTransaction(transaction);
            log.debug("Transaction stored: {}", savedTransaction.getTransactionId());

            // 2. Update customer summary
            customerSummaryService.updateCustomerSummary(transaction);
            log.debug("Customer summary updated for: {}", transaction.getCustomerId());

            // 3. Perform fraud detection
            boolean fraudDetected = fraudDetectionService.analyzeTransaction(transaction);
            if (fraudDetected) {
                fraudAlertsCounter.increment();
                log.warn("Fraud detected for transaction: {}", transaction.getTransactionId());
            }

            // 4. Update metrics
            metricsService.updateMetrics(transaction);

            processedTransactionsCounter.increment();
            log.info("Successfully processed transaction: {} for customer: {}",
                    transaction.getTransactionId(), transaction.getCustomerId());

        } catch (Exception e) {
            log.error("Error processing transaction {}: {}",
                    transaction.getTransactionId(), e.getMessage(), e);
            throw e;
        } finally {
            sample.stop(processingTimer);
        }
    }

    private TransactionEntity storeTransaction(Transaction transaction) {
        TransactionEntity entity = TransactionEntity.builder()
                .transactionId(transaction.getTransactionId())
                .customerId(transaction.getCustomerId())
                .accountNumber(transaction.getAccountNumber())
                .type(mapTransactionType(transaction.getType()))
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .merchantName(transaction.getMerchantName())
                .merchantCategory(transaction.getMerchantCategory())
                .description(transaction.getDescription())
                .status(mapTransactionStatus(transaction.getStatus()))
                .sourceLocation(transaction.getSourceLocation())
                .timestamp(transaction.getTimestamp())
                .ipAddress(transaction.getIpAddress())
                .deviceId(transaction.getDeviceId())
                .isInternational(transaction.getIsInternational())
                .riskScore(transaction.getRiskScore())
                .processedAt(LocalDateTime.now())
                .build();

        return transactionRepository.save(entity);
    }

    private TransactionEntity.TransactionType mapTransactionType(
            com.bankingplatform.streamprocessor.model.TransactionType type) {
        return TransactionEntity.TransactionType.valueOf(type.name());
    }

    private TransactionEntity.TransactionStatus mapTransactionStatus(
            com.bankingplatform.streamprocessor.model.TransactionStatus status) {
        return TransactionEntity.TransactionStatus.valueOf(status.name());
    }
}