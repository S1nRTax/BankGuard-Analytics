package com.bankingplatform.notificationservice.service;

import com.bankingplatform.notificationservice.entity.CustomerContactEntity;
import com.bankingplatform.notificationservice.entity.NotificationEntity;
import com.bankingplatform.notificationservice.model.NotificationResult;
import com.bankingplatform.notificationservice.model.NotificationStatus;
import com.bankingplatform.notificationservice.repository.CustomerContactRepository;
import com.bankingplatform.notificationservice.repository.NotificationRepository;
import com.bankingplatform.notificationservice.service.channels.EmailService;
import com.bankingplatform.notificationservice.service.channels.SmsService;
import com.bankingplatform.notificationservice.service.channels.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetryService {

    private final NotificationRepository notificationRepository;
    private final CustomerContactRepository customerContactRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final WebSocketService webSocketService;

    private static final int MAX_RETRY_ATTEMPTS = 3;

    @Scheduled(fixedDelay = 300000) // Run every 5 minutes
    @Transactional
    public void retryFailedNotifications() {
        log.info("Starting retry process for failed notifications...");

        List<NotificationEntity> failedNotifications =
                notificationRepository.findFailedNotificationsToRetry();

        if (failedNotifications.isEmpty()) {
            log.debug("No failed notifications to retry");
            return;
        }

        log.info("Found {} failed notifications to retry", failedNotifications.size());

        for (NotificationEntity notification : failedNotifications) {
            try {
                retryNotification(notification);
            } catch (Exception e) {
                log.error("Error during retry for notification {}: {}",
                        notification.getId(), e.getMessage(), e);
            }
        }

        log.info("Completed retry process");
    }

    private void retryNotification(NotificationEntity notification) {
        if (notification.getRetryCount() >= MAX_RETRY_ATTEMPTS) {
            log.warn("Maximum retry attempts reached for notification: {}, marking as failed",
                    notification.getId());
            notification.setStatus(NotificationStatus.FAILED);
            notificationRepository.save(notification);
            return;
        }

        Optional<CustomerContactEntity> contactOpt =
                customerContactRepository.findById(notification.getCustomerId());

        if (contactOpt.isEmpty()) {
            log.error("Customer contact not found for notification: {}", notification.getId());
            return;
        }

        CustomerContactEntity contact = contactOpt.get();
        notification.setStatus(NotificationStatus.RETRYING);
        notificationRepository.save(notification);

        log.info("Retrying notification: {} (attempt {})",
                notification.getId(), notification.getRetryCount() + 1);

        NotificationResult result = switch (notification.getChannel()) {
            case EMAIL -> emailService.sendEmail(notification, contact);
            case SMS -> smsService.sendSms(notification, contact);
            case WEBSOCKET -> webSocketService.sendWebSocketNotification(notification, contact);
            default -> NotificationResult.builder()
                    .notificationId(notification.getId())
                    .status(com.bankingplatform.notificationservice.model.NotificationStatus.FAILED)
                    .message("Unsupported channel for retry: " + notification.getChannel())
                    .build();
        };

        // Update notification based on retry result
        updateNotificationAfterRetry(notification, result);
    }

    private void updateNotificationAfterRetry(NotificationEntity notification, NotificationResult result) {
        notification.setRetryCount(notification.getRetryCount() + 1);

        if (result.getStatus() == com.bankingplatform.notificationservice.model.NotificationStatus.SENT) {
            notification.setStatus(NotificationStatus.SENT);
            notification.setExternalId(result.getExternalId());
            notification.setSentAt(result.getSentAt());
            notification.setErrorMessage(null);

            log.info("Retry successful for notification: {}", notification.getId());
        } else {
            if (notification.getRetryCount() >= MAX_RETRY_ATTEMPTS) {
                notification.setStatus(NotificationStatus.FAILED);
                log.warn("Final retry failed for notification: {}", notification.getId());
            } else {
                notification.setStatus(NotificationStatus.RETRYING);
            }
            notification.setErrorMessage(result.getMessage());
        }

        notificationRepository.save(notification);
    }
}