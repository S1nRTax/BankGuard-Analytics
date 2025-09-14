package com.bankingplatform.streamprocessor.controller;

import com.bankingplatform.streamprocessor.entity.TransactionMetricsEntity;
import com.bankingplatform.streamprocessor.service.MetricsAggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final MetricsAggregationService metricsService;

    @GetMapping("/realtime")
    public ResponseEntity<Map<String, Object>> getRealTimeMetrics() {
        Map<String, Object> metrics = metricsService.getRealTimeMetrics();
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/current")
    public ResponseEntity<TransactionMetricsEntity> getCurrentMetrics() {
        TransactionMetricsEntity metrics = metricsService.getCurrentMetrics();
        return metrics != null ? ResponseEntity.ok(metrics) : ResponseEntity.noContent().build();
    }

    @GetMapping("/recent")
    public ResponseEntity<List<TransactionMetricsEntity>> getRecentMetrics(
            @RequestParam(defaultValue = "24") int hours) {
        List<TransactionMetricsEntity> metrics = metricsService.getRecentMetrics(hours);
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/period")
    public ResponseEntity<List<TransactionMetricsEntity>> getMetricsForPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        List<TransactionMetricsEntity> metrics = metricsService.getMetricsForPeriod(start, end);
        return ResponseEntity.ok(metrics);
    }
}