package com.bankingplatform.transactiongenerator.scheduler;

import com.bankingplatform.transactiongenerator.config.TransactionGeneratorConfig;
import com.bankingplatform.transactiongenerator.model.Transaction;
import com.bankingplatform.transactiongenerator.service.TransactionGeneratorService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "transaction.generator.enabled", havingValue = "true", matchIfMissing = true)
public class TransactionGeneratorScheduler {

    private final TransactionGeneratorService transactionGeneratorService;
    private final TransactionGeneratorConfig config;

    @PostConstruct
    public void init(){
        transactionGeneratorService.init();
        log.info("Transaction generation scheduler initialized. " +
                        "Interval: {}ms, Batch size: {}",
                config.getIntervalMs(), config.getBatchSize());
    }

    @Scheduled(fixedRateString = "${transaction.generator.interval-ms:1000}")
    public void generateAndSendTransactions() {
        try {
            if (!config.isEnabled()) {
                return;
            }

            log.debug("Generating transaction batch...");
            List<Transaction> transactions = transactionGeneratorService.generateBatch();

            log.debug("Sending {} transactions to Kafka...", transactions.size());
            transactionGeneratorService.sendTransactionBatch(transactions);

            log.info("Successfully generated and sent {} transactions", transactions.size());

        } catch (Exception e) {
            log.error("Error during transaction generation: {}", e.getMessage(), e);
        }
    }

}
