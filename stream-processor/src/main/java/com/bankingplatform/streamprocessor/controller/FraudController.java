package com.bankingplatform.streamprocessor.controller;

import com.bankingplatform.streamprocessor.entity.FraudAlertEntity;
import com.bankingplatform.streamprocessor.service.FraudDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/fraud")
@RequiredArgsConstructor
public class FraudController {

    private final FraudDetectionService fraudDetectionService;

    @GetMapping("/alerts/active")
    public ResponseEntity<List<FraudAlertEntity>> getActiveAlerts() {
        List<FraudAlertEntity> alerts = fraudDetectionService.getActiveAlerts();
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/alerts/customer/{customerId}")
    public ResponseEntity<List<FraudAlertEntity>> getCustomerAlerts(
            @PathVariable String customerId) {
        List<FraudAlertEntity> alerts = fraudDetectionService.getCustomerAlerts(customerId);
        return ResponseEntity.ok(alerts);
    }

    @PutMapping("/alerts/{alertId}/status")
    public ResponseEntity<Map<String, String>> updateAlertStatus(
            @PathVariable String alertId,
            @RequestParam FraudAlertEntity.AlertStatus status) {

        fraudDetectionService.updateAlertStatus(alertId, status);

        log.info("Updated alert {} status to {}", alertId, status);

        return ResponseEntity.ok(Map.of(
                "alertId", alertId,
                "status", status.toString(),
                "message", "Alert status updated successfully"
        ));
    }
}