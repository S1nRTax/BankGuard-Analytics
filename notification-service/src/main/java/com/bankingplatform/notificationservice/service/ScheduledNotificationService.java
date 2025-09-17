package com.bankingplatform.notificationservice.service;

import com.bankingplatform.notificationservice.entity.NotificationEntity;
import com.bankingplatform.notificationservice.model.NotificationStatus;
import com.bankingplatform.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledNotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    @Scheduled(fixedDelay = 60000) // Run every minute
    @Transactional
    public void processScheduledNotifications() {
        log.debug("Checking for scheduled notifications...");

        List<NotificationEntity> readyNotifications =
                notificationRepository.findReadyToSend(LocalDateTime.now());

        if (readyNotifications.isEmpty()) {
            return;
        }

        log.info("Found {} scheduled notifications ready to send", readyNotifications.size());

        for (NotificationEntity notification : readyNotifications) {
            try {
                log.info("Processing scheduled notification: {} for customer: {}",
                        notification.getId(), notification.getCustomerId());

                // Mark as processing
                notification.setStatus(NotificationStatus.PENDING);
                notificationRepository.save(notification);

                // Process through notification service
                // todo: convert back to NotificationRequest and use the full pipeline

            } catch (Exception e) {
                log.error("Error processing scheduled notification {}: {}",
                        notification.getId(), e.getMessage(), e);

                notification.setStatus(NotificationStatus.FAILED);
                notification.setErrorMessage(e.getMessage());
                notificationRepository.save(notification);
            }
        }
    }

    @Scheduled(cron = "0 0 8 * * ?") // Daily at 8 AM
    public void sendDailySummaryNotifications() {
        log.info("Starting daily summary notification process...");

        // This would typically query for customers who have daily summaries enabled
        // and send them personalized summaries of their account activity

        // For demo purposes, we'll just log this
        log.info("Daily summary notifications would be sent here");
    }

    @Scheduled(cron = "0 0 0 * * ?") // Daily at midnight
    @Transactional
    public void cleanupOldNotifications() {
        log.info("Starting cleanup of old notifications...");

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        // In a real system, you'd implement a method to delete old notifications
        // For now, we'll just log this
        log.info("Would clean up notifications older than: {}", thirtyDaysAgo);
    }
}