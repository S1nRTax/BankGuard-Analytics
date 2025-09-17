package com.bankingplatform.notificationservice.controller;

import com.bankingplatform.notificationservice.entity.NotificationEntity;
import com.bankingplatform.notificationservice.model.NotificationRequest;
import com.bankingplatform.notificationservice.model.NotificationResult;
import com.bankingplatform.notificationservice.model.NotificationStatus;
import com.bankingplatform.notificationservice.repository.NotificationRepository;
import com.bankingplatform.notificationservice.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    @PostMapping("/send")
    public ResponseEntity<List<NotificationResult>> sendNotification(
            @Valid @RequestBody NotificationRequest request) {

        log.info("Received notification request for customer: {} of type: {}",
                request.getCustomerId(), request.getType());

        List<NotificationResult> results = notificationService.sendNotification(request);

        return ResponseEntity.ok(results);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<Page<NotificationEntity>> getCustomerNotifications(
            @PathVariable String customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationEntity> notifications =
                notificationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable);

        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/{notificationId}")
    public ResponseEntity<NotificationEntity> getNotification(@PathVariable String notificationId) {
        return notificationRepository.findById(notificationId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<NotificationEntity>> getNotificationsByStatus(
            @PathVariable NotificationStatus status) {

        List<NotificationEntity> notifications =
                notificationRepository.findByStatusOrderByCreatedAtAsc(status);

        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getNotificationStatistics(
            @RequestParam(defaultValue = "24") int hours) {

        LocalDateTime since = LocalDateTime.now().minusHours(hours);

        List<Object[]> statusStats = notificationRepository.getNotificationStatisticsSince(since);
        List<Object[]> typeStats = notificationRepository.getNotificationTypeStatisticsSince(since);

        Map<String, Object> stats = Map.of(
                "period", hours + " hours",
                "since", since.toString(),
                "statusStatistics", statusStats,
                "typeStatistics", typeStats
        );

        return ResponseEntity.ok(stats);
    }
}