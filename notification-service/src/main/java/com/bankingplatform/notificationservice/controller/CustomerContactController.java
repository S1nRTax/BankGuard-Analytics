package com.bankingplatform.notificationservice.controller;

import com.bankingplatform.notificationservice.entity.CustomerContactEntity;
import com.bankingplatform.notificationservice.repository.CustomerContactRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
public class CustomerContactController {

    private final CustomerContactRepository customerContactRepository;

    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerContactEntity> getCustomerContact(@PathVariable String customerId) {
        return customerContactRepository.findById(customerId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CustomerContactEntity> createOrUpdateContact(
            @Valid @RequestBody CustomerContactEntity contact) {

        contact.setUpdatedAt(LocalDateTime.now());
        if (contact.getCreatedAt() == null) {
            contact.setCreatedAt(LocalDateTime.now());
        }

        CustomerContactEntity saved = customerContactRepository.save(contact);
        log.info("Created/updated contact for customer: {}", saved.getCustomerId());

        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{customerId}/preferences")
    public ResponseEntity<CustomerContactEntity> updateNotificationPreferences(
            @PathVariable String customerId,
            @RequestBody Map<String, Boolean> preferences) {

        return customerContactRepository.findById(customerId)
                .map(contact -> {
                    preferences.forEach((key, value) -> {
                        switch (key) {
                            case "emailEnabled" -> contact.setEmailEnabled(value);
                            case "smsEnabled" -> contact.setSmsEnabled(value);
                            case "pushEnabled" -> contact.setPushEnabled(value);
                            case "fraudAlertsEnabled" -> contact.setFraudAlertsEnabled(value);
                            case "transactionAlertsEnabled" -> contact.setTransactionAlertsEnabled(value);
                            case "marketingEnabled" -> contact.setMarketingEnabled(value);
                        }
                    });

                    contact.setUpdatedAt(LocalDateTime.now());
                    CustomerContactEntity saved = customerContactRepository.save(contact);

                    log.info("Updated preferences for customer: {}", customerId);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getContactStatistics() {
        Long emailEnabled = customerContactRepository.countEmailEnabledCustomers();
        Long smsEnabled = customerContactRepository.countSmsEnabledCustomers();
        Long total = customerContactRepository.count();

        Map<String, Object> stats = Map.of(
                "totalCustomers", total,
                "emailEnabledCustomers", emailEnabled,
                "smsEnabledCustomers", smsEnabled,
                "emailEnabledPercentage", total > 0 ? (emailEnabled * 100.0) / total : 0,
                "smsEnabledPercentage", total > 0 ? (smsEnabled * 100.0) / total : 0
        );

        return ResponseEntity.ok(stats);
    }
}