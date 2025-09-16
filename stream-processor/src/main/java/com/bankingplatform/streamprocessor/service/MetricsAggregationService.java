package com.bankingplatform.streamprocessor.service;

import com.bankingplatform.streamprocessor.entity.TransactionMetricsEntity;
import com.bankingplatform.streamprocessor.model.Transaction;
import com.bankingplatform.streamprocessor.repository.FraudAlertRepository;
import com.bankingplatform.streamprocessor.repository.TransactionMetricsRepository;
import com.bankingplatform.streamprocessor.repository.TransactionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsAggregationService {

    private final TransactionMetricsRepository metricsRepository;
    private final TransactionRepository transactionRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final ObjectMapper objectMapper;

    // In-memory counters for real-time metrics (reset every minute)
    private final AtomicLong transactionCount = new AtomicLong(0);
    private final AtomicReference<BigDecimal> totalAmount = new AtomicReference<>(BigDecimal.ZERO);
    private final Map<String, AtomicLong> transactionsByType = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> transactionsByStatus = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> transactionsByLocation = new ConcurrentHashMap<>();
    private final AtomicReference<Double> totalRiskScore = new AtomicReference<>(0.0);
    private final AtomicLong alertsCount = new AtomicLong(0);

    public void updateMetrics(Transaction transaction) {
        // Update real-time counters
        transactionCount.incrementAndGet();

        // Update total amount atomically
        totalAmount.updateAndGet(current -> current.add(transaction.getAmount()));

        // Update type counters
        transactionsByType.computeIfAbsent(transaction.getType().toString(),
                k -> new AtomicLong(0)).incrementAndGet();

        // Update status counters
        transactionsByStatus.computeIfAbsent(transaction.getStatus().toString(),
                k -> new AtomicLong(0)).incrementAndGet();

        // Update location counters
        if (transaction.getSourceLocation() != null) {
            transactionsByLocation.computeIfAbsent(transaction.getSourceLocation(),
                    k -> new AtomicLong(0)).incrementAndGet();
        }

        // Update risk score
        if (transaction.getRiskScore() != null) {
            totalRiskScore.updateAndGet(current -> current + transaction.getRiskScore());
        }

        log.debug("Updated real-time metrics for transaction: {}", transaction.getTransactionId());
    }

    public void recordFraudAlert() {
        alertsCount.incrementAndGet();
    }

    // Scheduled job to aggregate and persist metrics every minute
    @Scheduled(fixedRate = 60000) // Every minute
    public void aggregateAndPersistMetrics() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime windowStart = now.truncatedTo(ChronoUnit.MINUTES);
            LocalDateTime windowEnd = windowStart.plusMinutes(1);

            // Capture current metrics and reset counters
            long totalTxns = transactionCount.getAndSet(0);
            BigDecimal totalAmt = totalAmount.getAndSet(BigDecimal.ZERO);
            Map<String, Long> typeMetrics = captureAndResetMap(transactionsByType);
            Map<String, Long> statusMetrics = captureAndResetMap(transactionsByStatus);
            Map<String, Long> locationMetrics = captureAndResetMap(transactionsByLocation);
            double totalRisk = totalRiskScore.getAndSet(0.0);
            long alerts = alertsCount.getAndSet(0);

            // Skip if no transactions in this window
            if (totalTxns == 0) {
                log.debug("No transactions in window {}, skipping metrics persistence", windowStart);
                return;
            }

            // Calculate average amount and risk score
            BigDecimal avgAmount = totalAmt.divide(BigDecimal.valueOf(totalTxns), 2, RoundingMode.HALF_UP);
            double avgRiskScore = totalTxns > 0 ? totalRisk / totalTxns : 0.0;

            // Create metrics entity
            TransactionMetricsEntity metricsEntity = TransactionMetricsEntity.builder()
                    .id(UUID.randomUUID())
                    .metricDate(windowStart.toLocalDate())
                    .windowStart(windowStart)
                    .windowEnd(windowEnd)
                    .totalTransactions(totalTxns)
                    .totalAmount(totalAmt)
                    .avgAmount(avgAmount)
                    .transactionsByType(mapToJson(typeMetrics))
                    .transactionsByStatus(mapToJson(statusMetrics))
                    .transactionsByLocation(mapToJson(locationMetrics))
                    .alertsGenerated(alerts)
                    .avgRiskScore(avgRiskScore)
                    .createdAt(now)
                    .build();

            metricsRepository.save(metricsEntity);


            metricsRepository.save(metricsEntity);

            log.info("Aggregated metrics for window {}: {} transactions, {} total amount, {} alerts",
                    windowStart, totalTxns, totalAmt, alerts);

        } catch (Exception e) {
            log.error("Error aggregating metrics: {}", e.getMessage(), e);
        }
    }

    // Scheduled job to generate hourly summary metrics
    @Scheduled(fixedRate = 3600000) // Every hour
    public void generateHourlySummary() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime oneHourAgo = now.minusHours(1);

            // Query database for historical metrics
            Long totalTransactions = transactionRepository.countTransactionsSince(oneHourAgo);
            BigDecimal totalAmount = transactionRepository.sumAmountSince(oneHourAgo);
            Long totalAlerts = fraudAlertRepository.countAlertsSince(oneHourAgo);

            log.info("Hourly Summary - Transactions: {}, Amount: {}, Alerts: {}",
                    totalTransactions, totalAmount, totalAlerts);

            // You could persist this summary to a separate table or send to monitoring system

        } catch (Exception e) {
            log.error("Error generating hourly summary: {}", e.getMessage(), e);
        }
    }

    private Map<String, Long> captureAndResetMap(Map<String, AtomicLong> atomicMap) {
        Map<String, Long> result = new HashMap<>();
        atomicMap.forEach((key, value) -> {
            long count = value.getAndSet(0);
            if (count > 0) {
                result.put(key, count);
            }
        });
        return result;
    }

    private String mapToJson(Map<String, Long> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error("Error converting map to JSON: {}", e.getMessage());
            return "{}";
        }
    }

    // API methods for retrieving metrics
    public TransactionMetricsEntity getCurrentMetrics() {
        return metricsRepository.findLatestWindowStart()
                .flatMap(metricsRepository::findByWindowStart)
                .orElse(null);
    }

    public List<TransactionMetricsEntity> getMetricsForPeriod(LocalDateTime start, LocalDateTime end) {
        return metricsRepository.findByWindowStartBetweenOrderByWindowStart(start, end);
    }

    public List<TransactionMetricsEntity> getRecentMetrics(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return metricsRepository.findRecentMetrics(since);
    }

    public Map<String, Object> getRealTimeMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("currentTransactionCount", transactionCount.get());
        metrics.put("currentTotalAmount", totalAmount.get());
        metrics.put("currentAlertCount", alertsCount.get());
        metrics.put("transactionsByType", getCurrentCountMap(transactionsByType));
        metrics.put("transactionsByStatus", getCurrentCountMap(transactionsByStatus));
        metrics.put("transactionsByLocation", getCurrentCountMap(transactionsByLocation));

        long txnCount = transactionCount.get();
        if (txnCount > 0) {
            double avgRisk = totalRiskScore.get() / txnCount;
            metrics.put("currentAvgRiskScore", avgRisk);
        }

        return metrics;
    }

    private Map<String, Long> getCurrentCountMap(Map<String, AtomicLong> atomicMap) {
        Map<String, Long> result = new HashMap<>();
        atomicMap.forEach((key, value) -> result.put(key, value.get()));
        return result;
    }
}