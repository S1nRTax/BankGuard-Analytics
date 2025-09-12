package com.bankingplatform.transactiongenerator.service;

import com.bankingplatform.transactiongenerator.config.TransactionGeneratorConfig;
import com.bankingplatform.transactiongenerator.model.Transaction;
import com.bankingplatform.transactiongenerator.model.TransactionStatus;
import com.bankingplatform.transactiongenerator.model.TransactionType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;


@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionGeneratorService {

    private final TransactionGeneratorConfig config;
    private final TransactionProducer transactionProducer;
    private final MeterRegistry meterRegistry;

    // metrics
    private Counter transactionsGenerated;
    private Counter transactionsSent;

    // Predefined customer pool for realistic simulation
    private final List<String> customerPool = generateCustomerPool();
    private final List<String> accountPool = generateAccountPool();


    public void init() {
        this.transactionsGenerated = Counter.builder("transaction_generated_total")
                .description("Total number of transactions generated")
                .register(meterRegistry);

        this.transactionsSent = Counter.builder("transactions_sent_total")
                .description("Total number of transactions sent to Kafka")
                .register(meterRegistry);
    }


    public List<Transaction> generateBatch() {
        List<Transaction> transactions = new ArrayList<>();

        for (int i = 0; i < config.getBatchSize(); i++) {
            Transaction transaction = generateSingleTransaction();
            transactions.add(transaction);
            transactionsGenerated.increment();
        }

        log.info("Generated batch of {} transactions", transactions.size());
        return transactions;
    }

    public Transaction generateSingleTransaction() {
        TransactionType type = selectRandomTransactionType();
        String customerId = getRandomCustomer();

        return Transaction.builder()
                .transactionId(generateTransactionId())
                .customerId(customerId)
                .accountNumber(getRandomAccount())
                .type(type)
                .amount(generateRealisticAmount(type))
                .currency(selectRandomCurrency())
                .merchantName(selectRandomMerchant())
                .merchantCategory(selectRandomMerchantCategory())
                .description(generateDescription(type))
                .status(selectTransactionStatus())
                .sourceLocation(selectRandomLocation())
                .timestamp(LocalDateTime.now())
                .ipAddress(generateRandomIP())
                .deviceId(generateDeviceId())
                .isInternational(ThreadLocalRandom.current().nextBoolean())
                .riskScore(calculateRiskScore())
                .build();
    }

    public void sendTransactionBatch(List<Transaction> transactions) {
        transactions.forEach(transaction -> {
            transactionProducer.sendTransaction(transaction)
                    .whenComplete((result, throwable) -> {
                        if (throwable == null) {
                            transactionsSent.increment();
                        }
                    });
        });
    }

    // Private helper methods
    private TransactionType selectRandomTransactionType() {
        Map<String, Integer> distribution = config.getTypeDistribution();
        int totalWeight = distribution.values().stream().mapToInt(Integer::intValue).sum();
        int randomValue = ThreadLocalRandom.current().nextInt(totalWeight);

        int currentWeight = 0;
        for (Map.Entry<String, Integer> entry : distribution.entrySet()) {
            currentWeight += entry.getValue();
            if (randomValue < currentWeight) {
                return TransactionType.valueOf(entry.getKey());
            }
        }
        return TransactionType.PAYMENT; // fallback
    }


    private BigDecimal generateRealisticAmount(TransactionType type) {
        double amount = switch (type) {
            case PAYMENT ->
                // Most payments are small: 50-2000 MAD
                    ThreadLocalRandom.current().nextDouble(50, 2000);
            case TRANSFER ->
                // Transfers can be larger: 100-10000 MAD
                    ThreadLocalRandom.current().nextDouble(100, 10000);
            case WITHDRAWAL ->
                // ATM withdrawals: 100-2000 MAD
                    ThreadLocalRandom.current().nextDouble(100, 2000);
            case DEPOSIT ->
                // Deposits: 500-50000 MAD
                    ThreadLocalRandom.current().nextDouble(500, 50000);
            default -> ThreadLocalRandom.current().nextDouble(100, 1000);
        };

        // Add some high-value transactions (1% chance)
        if (ThreadLocalRandom.current().nextDouble() < 0.01) {
            amount *= ThreadLocalRandom.current().nextDouble(10, 50);
        }

        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
    }

    private String selectRandomCurrency() {
        List<String> currencies = config.getCurrencies();
        return currencies.get(ThreadLocalRandom.current().nextInt(currencies.size()));
    }

    private String selectRandomMerchant() {
        List<String> merchants = config.getMoroccanMerchants();
        return merchants.get(ThreadLocalRandom.current().nextInt(merchants.size()));
    }

    private String selectRandomMerchantCategory() {
        List<String> categories = config.getMerchantCategories();
        return categories.get(ThreadLocalRandom.current().nextInt(categories.size()));
    }

    private String selectRandomLocation() {
        List<String> cities = config.getMoroccanCities();
        return cities.get(ThreadLocalRandom.current().nextInt(cities.size()));
    }

    private TransactionStatus selectTransactionStatus() {
        // 85% completed, 10% pending, 3% failed, 2% processing
        double random = ThreadLocalRandom.current().nextDouble();
        if (random < 0.85) return TransactionStatus.COMPLETED;
        if (random < 0.95) return TransactionStatus.PENDING;
        if (random < 0.98) return TransactionStatus.FAILED;
        return TransactionStatus.PROCESSING;
    }


    private String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() +
                ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    private String generateDescription(TransactionType type) {
        return switch (type) {
            case PAYMENT -> "Payment to merchant";
            case TRANSFER -> "Transfer to account";
            case WITHDRAWAL -> "ATM withdrawal";
            case DEPOSIT -> "Account deposit";
            case REFUND -> "Transaction refund";
            case SUBSCRIPTION -> "Subscription payment";
            case INVESTMENT -> "Investment transaction";
        };
    }

    private String generateRandomIP() {
        return ThreadLocalRandom.current().nextInt(1, 255) + "." +
                ThreadLocalRandom.current().nextInt(1, 255) + "." +
                ThreadLocalRandom.current().nextInt(1, 255) + "." +
                ThreadLocalRandom.current().nextInt(1, 255);
    }

    private String generateDeviceId() {
        return "DEV" + ThreadLocalRandom.current().nextInt(100000, 999999);
    }


    private Double calculateRiskScore() {
        // Simple risk scoring (0.0 to 1.0)
        return ThreadLocalRandom.current().nextDouble(0.0, 1.0);
    }

    private String getRandomCustomer() {
        return customerPool.get(ThreadLocalRandom.current().nextInt(customerPool.size()));
    }

    private String getRandomAccount() {
        return accountPool.get(ThreadLocalRandom.current().nextInt(accountPool.size()));
    }

    private List<String> generateCustomerPool() {
        List<String> customers = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) {
            customers.add("CUST" + String.format("%06d", i));
        }
        return customers;
    }

    private List<String> generateAccountPool() {
        List<String> accounts = new ArrayList<>();
        for (int i = 1; i <= 1500; i++) {
            accounts.add("ACC" + String.format("%08d", i));
        }
        return accounts;
    }


}
