package com.bankingplatform.streamprocessor.controller;

import com.bankingplatform.streamprocessor.entity.CustomerSummaryEntity;
import com.bankingplatform.streamprocessor.entity.TransactionEntity;
import com.bankingplatform.streamprocessor.repository.TransactionRepository;
import com.bankingplatform.streamprocessor.service.CustomerSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerSummaryService customerSummaryService;
    private final TransactionRepository transactionRepository;

    @GetMapping("/{customerId}/summary")
    public ResponseEntity<CustomerSummaryEntity> getCustomerSummary(
            @PathVariable String customerId) {

        Optional<CustomerSummaryEntity> summary =
                customerSummaryService.getCustomerSummaryById(customerId);

        return summary.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{customerId}/transactions")
    public ResponseEntity<List<TransactionEntity>> getCustomerTransactions(
            @PathVariable String customerId,
            @RequestParam(defaultValue = "50") int limit) {

        List<TransactionEntity> transactions = transactionRepository
                .findByCustomerIdOrderByTimestampDesc(customerId)
                .stream()
                .limit(limit)
                .toList();

        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/high-value")
    public ResponseEntity<List<CustomerSummaryEntity>> getHighValueCustomers(
            @RequestParam(defaultValue = "10000") BigDecimal minAmount) {

        List<CustomerSummaryEntity> customers =
                customerSummaryService.getHighValueCustomers(minAmount);

        return ResponseEntity.ok(customers);
    }

    @GetMapping("/high-risk")
    public ResponseEntity<List<CustomerSummaryEntity>> getHighRiskCustomers(
            @RequestParam(defaultValue = "0.7") Double riskThreshold) {

        List<CustomerSummaryEntity> customers =
                customerSummaryService.getHighRiskCustomers(riskThreshold);

        return ResponseEntity.ok(customers);
    }
}