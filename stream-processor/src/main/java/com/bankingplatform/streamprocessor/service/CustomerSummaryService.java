package com.bankingplatform.streamprocessor.service;

import com.bankingplatform.streamprocessor.entity.CustomerSummaryEntity;
import com.bankingplatform.streamprocessor.model.Transaction;
import com.bankingplatform.streamprocessor.model.TransactionStatus;
import com.bankingplatform.streamprocessor.repository.CustomerSummaryRepository;
import com.bankingplatform.streamprocessor.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerSummaryService {

    private final CustomerSummaryRepository customerSummaryRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public void updateCustomerSummary(Transaction transaction) {
        String customerId = transaction.getCustomerId();

        Optional<CustomerSummaryEntity> existingSummary =
                customerSummaryRepository.findById(customerId);

        CustomerSummaryEntity summary;
        if (existingSummary.isPresent()) {
            summary = existingSummary.get();
            updateExistingSummary(summary, transaction);
        } else {
            summary = createNewSummary(customerId, transaction);
        }

        // Calculate time-window metrics
        updateTimeWindowMetrics(summary, customerId);

        summary.setUpdatedAt(LocalDateTime.now());
        customerSummaryRepository.save(summary);

        log.debug("Updated customer summary for: {}", customerId);
    }

    private void updateExistingSummary(CustomerSummaryEntity summary, Transaction transaction) {
        // Update total transactions
        summary.setTotalTransactions(summary.getTotalTransactions() + 1);

        // Update amounts (only for completed transactions)
        if (transaction.getStatus() == TransactionStatus.COMPLETED) {
            BigDecimal newTotal = summary.getTotalAmount().add(transaction.getAmount());
            summary.setTotalAmount(newTotal);

            BigDecimal newAvg = newTotal.divide(
                    BigDecimal.valueOf(summary.getTotalTransactions()),
                    2, RoundingMode.HALF_UP);
            summary.setAvgAmount(newAvg);
        }

        // Update last transaction time
        if (transaction.getTimestamp().isAfter(summary.getLastTransactionTime())) {
            summary.setLastTransactionTime(transaction.getTimestamp());
        }

        // Update most frequent merchant category
        updateMostFrequentMerchantCategory(summary);

        // Update average risk score
        updateAverageRiskScore(summary);
    }

    private CustomerSummaryEntity createNewSummary(String customerId, Transaction transaction) {
        return CustomerSummaryEntity.builder()
                .customerId(customerId)
                .totalTransactions(1L)
                .totalAmount(transaction.getStatus() == TransactionStatus.COMPLETED ?
                        transaction.getAmount() : BigDecimal.ZERO)
                .avgAmount(transaction.getAmount())
                .lastTransactionTime(transaction.getTimestamp())
                .avgRiskScore(transaction.getRiskScore())
                .transactionsLast1Hour(0L)
                .amountLast1Hour(BigDecimal.ZERO)
                .transactionsLast24Hours(0L)
                .amountLast24Hours(BigDecimal.ZERO)
                .build();
    }

    private void updateTimeWindowMetrics(CustomerSummaryEntity summary, String customerId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        LocalDateTime twentyFourHoursAgo = now.minusHours(24);

        // Last 1 hour metrics
        Long transactionsLast1Hour = transactionRepository
                .countByCustomerIdAndTimestampAfter(customerId, oneHourAgo);
        BigDecimal amountLast1Hour = transactionRepository
                .sumAmountByCustomerIdAndTimestampAfter(customerId, oneHourAgo);

        summary.setTransactionsLast1Hour(transactionsLast1Hour);
        summary.setAmountLast1Hour(amountLast1Hour != null ? amountLast1Hour : BigDecimal.ZERO);

        // Last 24 hours metrics
        Long transactionsLast24Hours = transactionRepository
                .countByCustomerIdAndTimestampAfter(customerId, twentyFourHoursAgo);
        BigDecimal amountLast24Hours = transactionRepository
                .sumAmountByCustomerIdAndTimestampAfter(customerId, twentyFourHoursAgo);

        summary.setTransactionsLast24Hours(transactionsLast24Hours);
        summary.setAmountLast24Hours(amountLast24Hours != null ? amountLast24Hours : BigDecimal.ZERO);
    }

    private void updateMostFrequentMerchantCategory(CustomerSummaryEntity summary) {
        List<Object[]> results = transactionRepository
                .findMostFrequentMerchantCategoryByCustomerId(summary.getCustomerId());

        if (!results.isEmpty()) {
            Object[] mostFrequent = results.get(0);
            summary.setMostFrequentMerchantCategory((String) mostFrequent[0]);
        }
    }

    private void updateAverageRiskScore(CustomerSummaryEntity summary) {
        Double avgRiskScore = transactionRepository
                .findAvgRiskScoreByCustomerId(summary.getCustomerId());

        if (avgRiskScore != null) {
            summary.setAvgRiskScore(avgRiskScore);
        }
    }

    public List<CustomerSummaryEntity> getHighValueCustomers(BigDecimal minAmount) {
        return customerSummaryRepository.findHighValueCustomers(minAmount);
    }

    public List<CustomerSummaryEntity> getHighRiskCustomers(Double riskThreshold) {
        return customerSummaryRepository.findHighRiskCustomers(riskThreshold);
    }

    public Optional<CustomerSummaryEntity> getCustomerSummaryById(String customerId) {
        return customerSummaryRepository.findById(customerId);
    }
}