package com.bankingplatform.transactiongenerator.controller;

import com.bankingplatform.transactiongenerator.config.TransactionGeneratorConfig;
import com.bankingplatform.transactiongenerator.model.Transaction;
import com.bankingplatform.transactiongenerator.service.TransactionGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionGeneratorService transactionGeneratorService;
    private final TransactionGeneratorConfig config;

    @PostMapping("/generate")
    public ResponseEntity<List<Transaction>> generateTransactions(
            @RequestParam(defaultValue = "1") int count) {

        log.info("Manual generation requested for {} transactions", count);

        // Override batch size temporarily
        int originalBatchSize = config.getBatchSize();
        config.setBatchSize(count);

        try {
            List<Transaction> transactions = transactionGeneratorService.generateBatch();
            transactionGeneratorService.sendTransactionBatch(transactions);
            return ResponseEntity.ok(transactions);
        } finally {
            config.setBatchSize(originalBatchSize);
        }
    }

    @PostMapping("/generate/single")
    public ResponseEntity<Transaction> generateSingleTransaction() {
        log.info("Single transaction generation requested");

        Transaction transaction = transactionGeneratorService.generateSingleTransaction();
        transactionGeneratorService.sendTransactionBatch(List.of(transaction));

        return ResponseEntity.ok(transaction);
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        return ResponseEntity.ok(Map.of(
                "enabled", config.isEnabled(),
                "intervalMs", config.getIntervalMs(),
                "batchSize", config.getBatchSize(),
                "defaultCurrency", config.getDefaultCurrency(),
                "typeDistribution", config.getTypeDistribution()
        ));
    }

    @PostMapping("/config/toggle")
    public ResponseEntity<Map<String, Boolean>> toggleGeneration() {
        boolean newState = !config.isEnabled();
        config.setEnabled(newState);

        log.info("Transaction generation {}", newState ? "enabled" : "disabled");

        return ResponseEntity.ok(Map.of("enabled", newState));
    }

    @PostMapping("/config/interval")
    public ResponseEntity<Map<String, Integer>> updateInterval(
            @RequestParam int intervalMs) {

        if (intervalMs < 100) {
            return ResponseEntity.badRequest().build();
        }

        config.setIntervalMs(intervalMs);
        log.info("Updated generation interval to {}ms", intervalMs);

        return ResponseEntity.ok(Map.of("intervalMs", intervalMs));
    }
}