package com.bankingplatform.streamprocessor.service;

import com.bankingplatform.streamprocessor.entity.CustomerSummaryEntity;
import com.bankingplatform.streamprocessor.entity.FraudAlertEntity;
import com.bankingplatform.streamprocessor.model.Transaction;
import com.bankingplatform.streamprocessor.repository.CustomerSummaryRepository;
import com.bankingplatform.streamprocessor.repository.FraudAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final FraudAlertRepository fraudAlertRepository;
    private final CustomerSummaryRepository customerSummaryRepository;
    private final NotificationService notificationService;

    // Fraud detection thresholds
    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("10000.00");
    private static final Long FREQUENT_TRANSACTIONS_THRESHOLD = 10L; // per hour
    private static final BigDecimal FREQUENT_AMOUNT_THRESHOLD = new BigDecimal("15000.00"); // per hour
    private static final Double HIGH_RISK_SCORE_THRESHOLD = 0.8;
    private static final BigDecimal VELOCITY_AMOUNT_THRESHOLD = new BigDecimal("20000.00"); // per 24h

    public boolean analyzeTransaction(Transaction transaction) {
        List<FraudAlertEntity> alerts = new ArrayList<>();

        try {
            // Rule 1: High amount detection
            if (isHighAmountTransaction(transaction)) {
                alerts.add(createAlert(transaction,
                        FraudAlertEntity.FraudReason.HIGH_AMOUNT,
                        "Transaction amount exceeds threshold: " + transaction.getAmount(),
                        0.7));
            }

            // Rule 2: High risk score
            if (isHighRiskTransaction(transaction)) {
                alerts.add(createAlert(transaction,
                        FraudAlertEntity.FraudReason.HIGH_RISK_SCORE,
                        "Transaction has high risk score: " + transaction.getRiskScore(),
                        transaction.getRiskScore()));
            }

            // Rule 3: Frequent transactions (velocity check)
            if (isFrequentTransactionPattern(transaction)) {
                alerts.add(createAlert(transaction,
                        FraudAlertEntity.FraudReason.FREQUENT_TRANSACTIONS,
                        "Customer has too many transactions in short time",
                        0.6));
            }

            // Rule 4: Velocity check - high amount in short time
            if (isVelocityCheckFailed(transaction)) {
                alerts.add(createAlert(transaction,
                        FraudAlertEntity.FraudReason.VELOCITY_CHECK_FAILED,
                        "Customer exceeded spending velocity limits",
                        0.8));
            }

            // Rule 5: Unusual location (simplified)
            if (isUnusualLocation(transaction)) {
                alerts.add(createAlert(transaction,
                        FraudAlertEntity.FraudReason.UNUSUAL_LOCATION,
                        "Transaction from unusual location: " + transaction.getSourceLocation(),
                        0.5));
            }

            // Rule 6: Suspicious pattern detection
            if (isSuspiciousPattern(transaction)) {
                alerts.add(createAlert(transaction,
                        FraudAlertEntity.FraudReason.SUSPICIOUS_PATTERN,
                        "Suspicious transaction pattern detected",
                        0.7));
            }

            // Save all alerts
            if (!alerts.isEmpty()) {
                fraudAlertRepository.saveAll(alerts);

                // Send notifications for high-severity alerts
                alerts.stream()
                        .filter(alert -> alert.getSeverity() > 0.7)
                        .forEach(alert -> {
                            try {
                                notificationService.sendFraudAlert(alert);
                            } catch (Exception e) {
                                log.error("Failed to send fraud alert notification: {}", e.getMessage());
                            }
                        });

                log.warn("Generated {} fraud alerts for transaction: {}",
                        alerts.size(), transaction.getTransactionId());

                return true;
            }

            return false;

        } catch (Exception e) {
            log.error("Error during fraud detection for transaction {}: {}",
                    transaction.getTransactionId(), e.getMessage(), e);
            return false;
        }
    }

    private boolean isHighAmountTransaction(Transaction transaction) {
        return transaction.getAmount().compareTo(HIGH_AMOUNT_THRESHOLD) > 0;
    }

    private boolean isHighRiskTransaction(Transaction transaction) {
        return transaction.getRiskScore() != null &&
                transaction.getRiskScore() > HIGH_RISK_SCORE_THRESHOLD;
    }

    private boolean isFrequentTransactionPattern(Transaction transaction) {
        Optional<CustomerSummaryEntity> summaryOpt =
                customerSummaryRepository.findById(transaction.getCustomerId());

        if (summaryOpt.isPresent()) {
            CustomerSummaryEntity summary = summaryOpt.get();

            // Check if customer has too many transactions in last hour
            boolean tooManyTransactions = summary.getTransactionsLast1Hour() > FREQUENT_TRANSACTIONS_THRESHOLD;

            // Check if customer spent too much in last hour
            boolean tooMuchAmount = summary.getAmountLast1Hour() != null &&
                    summary.getAmountLast1Hour().compareTo(FREQUENT_AMOUNT_THRESHOLD) > 0;

            return tooManyTransactions || tooMuchAmount;
        }

        return false;
    }

    private boolean isVelocityCheckFailed(Transaction transaction) {
        Optional<CustomerSummaryEntity> summaryOpt =
                customerSummaryRepository.findById(transaction.getCustomerId());

        if (summaryOpt.isPresent()) {
            CustomerSummaryEntity summary = summaryOpt.get();

            // Check if customer exceeded 24-hour spending limit
            return summary.getAmountLast24Hours() != null &&
                    summary.getAmountLast24Hours().compareTo(VELOCITY_AMOUNT_THRESHOLD) > 0;
        }

        return false;
    }

    private boolean isUnusualLocation(Transaction transaction) {
        Optional<CustomerSummaryEntity> summaryOpt =
                customerSummaryRepository.findById(transaction.getCustomerId());

        if (summaryOpt.isPresent()) {
            CustomerSummaryEntity summary = summaryOpt.get();

            // Simple check: if preferred location is set and current location is different
            return summary.getPreferredLocation() != null &&
                    !summary.getPreferredLocation().equals(transaction.getSourceLocation());
        }

        return false;
    }

    private boolean isSuspiciousPattern(Transaction transaction) {
        // Check for round amounts (often suspicious)
        boolean isRoundAmount = transaction.getAmount().remainder(BigDecimal.valueOf(100))
                .compareTo(BigDecimal.ZERO) == 0;

        // Check for international transactions with high risk
        boolean suspiciousInternational = transaction.getIsInternational() != null &&
                transaction.getIsInternational() &&
                transaction.getRiskScore() != null &&
                transaction.getRiskScore() > 0.6;

        // Check for late-night transactions (simplified)
        LocalDateTime timestamp = transaction.getTimestamp();
        boolean lateNightTransaction = timestamp.getHour() >= 22 || timestamp.getHour() <= 5;

        return (isRoundAmount && transaction.getAmount().compareTo(new BigDecimal("1000")) > 0) ||
                suspiciousInternational ||
                (lateNightTransaction && transaction.getAmount().compareTo(new BigDecimal("5000")) > 0);
    }

    private FraudAlertEntity createAlert(Transaction transaction,
                                         FraudAlertEntity.FraudReason reason,
                                         String description,
                                         Double severity) {
        return FraudAlertEntity.builder()
                .alertId(generateAlertId())
                .customerId(transaction.getCustomerId())
                .transactionId(transaction.getTransactionId())
                .reason(reason)
                .description(description)
                .severity(severity)
                .amount(transaction.getAmount())
                .timestamp(transaction.getTimestamp())
                .status(FraudAlertEntity.AlertStatus.NEW)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private String generateAlertId() {
        return "ALERT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public List<FraudAlertEntity> getActiveAlerts() {
        return fraudAlertRepository.findByStatusOrderByTimestampDesc(
                FraudAlertEntity.AlertStatus.NEW);
    }

    public List<FraudAlertEntity> getCustomerAlerts(String customerId) {
        return fraudAlertRepository.findByCustomerIdOrderByTimestampDesc(customerId);
    }

    public void updateAlertStatus(String alertId, FraudAlertEntity.AlertStatus status) {
        fraudAlertRepository.findById(alertId).ifPresent(alert -> {
            alert.setStatus(status);
            fraudAlertRepository.save(alert);
            log.info("Updated alert {} status to {}", alertId, status);
        });
    }
}