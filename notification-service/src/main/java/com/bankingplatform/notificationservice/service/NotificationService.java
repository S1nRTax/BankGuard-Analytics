// NotificationService.java
package com.bankingplatform.notificationservice.service;

import com.bankingplatform.notificationservice.entity.CustomerContactEntity;
import com.bankingplatform.notificationservice.entity.NotificationEntity;
import com.bankingplatform.notificationservice.model.*;
import com.bankingplatform.notificationservice.repository.CustomerContactRepository;
import com.bankingplatform.notificationservice.repository.NotificationRepository;
import com.bankingplatform.notificationservice.service.channels.EmailService;
import com.bankingplatform.notificationservice.service.channels.SmsService;
import com.bankingplatform.notificationservice.service.channels.WebSocketService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bankingplatform.notificationservice.model.NotificationChannel;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CustomerContactRepository customerContactRepository;
    private final TemplateService templateService;
    private final EmailService emailService;
    private final SmsService smsService;
    private final WebSocketService webSocketService;
    private final MeterRegistry meterRegistry;

    // Metrics
    private Counter notificationsProcessed;
    private Counter notificationsSent;
    private Counter notificationsFailed;

    @PostConstruct
    public void initMetrics() {
        this.notificationsProcessed = Counter.builder("notifications_processed_total")
                .description("Total number of notifications processed")
                .register(meterRegistry);

        this.notificationsSent = Counter.builder("notifications_sent_total")
                .description("Total number of notifications sent successfully")
                .tag("channel", "all")
                .register(meterRegistry);

        this.notificationsFailed = Counter.builder("notifications_failed_total")
                .description("Total number of failed notifications")
                .register(meterRegistry);
    }

    @Transactional
    public List<NotificationResult> sendNotification(NotificationRequest request) {
        notificationsProcessed.increment();

        try {
            log.info("Processing notification request for customer: {} of type: {}",
                    request.getCustomerId(), request.getType());

            // Get customer contact information
            Optional<CustomerContactEntity> contactOpt =
                    customerContactRepository.findByCustomerId(request.getCustomerId());

            if (contactOpt.isEmpty()) {
                log.warn("No contact information found for customer: {}", request.getCustomerId());
                return List.of(NotificationResult.builder()
                        .status(NotificationStatus.FAILED)
                        .message("Customer contact information not found")
                        .build());
            }

            CustomerContactEntity contact = contactOpt.get();
            List<NotificationResult> results = new ArrayList<>();

            // Determine which channels to use
            List<NotificationChannel> channels =
                    determineChannels(request, contact);

            // Send notification through each channel
            for (NotificationChannel channel : channels) {
                try {
                    NotificationResult result = sendToChannel(request, contact, channel);
                    results.add(result);

                    if (result.getStatus() == NotificationStatus.SENT) {
                        notificationsSent.increment();
                    } else {
                        notificationsFailed.increment();
                    }

                } catch (Exception e) {
                    log.error("Failed to send notification via {}: {}", channel, e.getMessage(), e);
                    notificationsFailed.increment();

                    results.add(NotificationResult.builder()
                            .status(NotificationStatus.FAILED)
                            .message("Channel error: " + e.getMessage())
                            .build());
                }
            }

            log.info("Completed notification processing for customer: {} - {} results",
                    request.getCustomerId(), results.size());

            return results;

        } catch (Exception e) {
            log.error("Error processing notification request: {}", e.getMessage(), e);
            notificationsFailed.increment();

            return List.of(NotificationResult.builder()
                    .status(NotificationStatus.FAILED)
                    .message("Processing error: " + e.getMessage())
                    .build());
        }
    }

    private List<NotificationChannel> determineChannels(
            NotificationRequest request, CustomerContactEntity contact) {

        List<NotificationChannel> channels = new ArrayList<>();

        if (request.getChannel() == NotificationChannel.ALL) {
            // Send to all enabled channels based on notification type and customer preferences
            if (shouldSendEmail(request, contact)) {
                channels.add(NotificationChannel.EMAIL);
            }
            if (shouldSendSms(request, contact)) {
                channels.add(NotificationChannel.SMS);
            }
            if (shouldSendWebSocket(request, contact)) {
                channels.add(NotificationChannel.WEBSOCKET);
            }
        } else {
            // Use specified channel if customer has it enabled
            NotificationChannel targetChannel =
                    mapToEntityChannel(request.getChannel());
            if (isChannelEnabled(targetChannel, contact)) {
                channels.add(targetChannel);
            }
        }

        return channels;
    }

    private boolean shouldSendEmail(NotificationRequest request, CustomerContactEntity contact) {
        return contact.getEmailEnabled() &&
                contact.getEmail() != null &&
                !contact.getEmail().trim().isEmpty() &&
                isNotificationTypeEnabled(request.getType(), contact);
    }

    private boolean shouldSendSms(NotificationRequest request, CustomerContactEntity contact) {
        return contact.getSmsEnabled() &&
                contact.getPhoneNumber() != null &&
                !contact.getPhoneNumber().trim().isEmpty() &&
                isNotificationTypeEnabled(request.getType(), contact);
    }

    private boolean shouldSendWebSocket(NotificationRequest request, CustomerContactEntity contact) {
        // WebSocket is always enabled for real-time notifications
        return request.getPriority() == NotificationPriority.URGENT ||
                request.getPriority() == NotificationPriority.HIGH;
    }

    private boolean isNotificationTypeEnabled(NotificationType type,CustomerContactEntity contact) {
        return switch (type) {
            case FRAUD_ALERT -> contact.getFraudAlertsEnabled();
            case TRANSACTION_CONFIRMATION -> contact.getTransactionAlertsEnabled();
            case MARKETING -> contact.getMarketingEnabled();
            default -> true; // System notifications are always enabled
        };
    }

    private boolean isChannelEnabled(NotificationChannel channel,
                                     CustomerContactEntity contact) {
        return switch (channel) {
            case EMAIL -> shouldSendEmail(null, contact);
            case SMS -> shouldSendSms(null, contact);
            case WEBSOCKET -> true;
            default -> false;
        };
    }

    private NotificationResult sendToChannel(NotificationRequest request,
                                             CustomerContactEntity contact,
                                             NotificationChannel channel) {

        // Get and render template
        String renderedSubject;
        String renderedMessage;
        String renderedHtmlMessage = null;

        if (request.getTemplateData() != null) {
            // Use template
            Optional<String> template = templateService.renderTemplate(
                    mapToTemplateType(request.getType()),
                    channel,
                    contact.getPreferredLanguage(),
                    request.getTemplateData()
            );

            if (template.isPresent()) {
                renderedMessage = template.get();
                renderedSubject = templateService.renderSubject(
                        mapToTemplateType(request.getType()),
                        channel,
                        contact.getPreferredLanguage(),
                        request.getTemplateData()
                ).orElse(request.getSubject());
            } else {
                renderedMessage = request.getMessage() != null ? request.getMessage() : "Notification";
                renderedSubject = request.getSubject() != null ? request.getSubject() : "Banking Alert";
            }
        } else {
            renderedMessage = request.getMessage() != null ? request.getMessage() : "Notification";
            renderedSubject = request.getSubject() != null ? request.getSubject() : "Banking Alert";
        }

        // Create notification entity
        NotificationEntity notification = NotificationEntity.builder()
                .customerId(request.getCustomerId())
                .type(mapToEntityType(request.getType()))
                .channel(channel)
                .subject(renderedSubject)
                .message(renderedMessage)
                .htmlMessage(renderedHtmlMessage)
                .priority(mapToEntityPriority(request.getPriority()))
                .status(NotificationStatus.PENDING)
                .recipient(getRecipientAddress(channel, contact))
                .scheduledTime(request.getScheduledTime() != null ?
                        request.getScheduledTime() : LocalDateTime.now())
                .retryCount(0)
                .build();

        // Save notification
        notification = notificationRepository.save(notification);

        // Send through appropriate channel
        NotificationResult result = switch (channel) {
            case EMAIL -> emailService.sendEmail(notification, contact);
            case SMS -> smsService.sendSms(notification, contact);
            case WEBSOCKET -> webSocketService.sendWebSocketNotification(notification, contact);
            default -> NotificationResult.builder()
                    .notificationId(notification.getId())
                    .status(NotificationStatus.FAILED)
                    .message("Unsupported channel: " + channel)
                    .build();
        };

        // Update notification status
        updateNotificationStatus(notification.getId(), result);

        return result;
    }

    private void updateNotificationStatus(String notificationId, NotificationResult result) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setStatus(mapToEntityStatus(result.getStatus()));
            notification.setExternalId(result.getExternalId());
            notification.setSentAt(result.getSentAt());

            if (result.getStatus() == NotificationStatus.FAILED) {
                notification.setErrorMessage(result.getMessage());
                notification.setRetryCount(notification.getRetryCount() + 1);
            }

            notificationRepository.save(notification);
        });
    }

    // Mapping helper methods
    private NotificationChannel mapToEntityChannel(NotificationChannel channel) {
        return NotificationChannel.valueOf(channel.name());
    }

    private NotificationType mapToEntityType(NotificationType type) {
        return NotificationType.valueOf(type.name());
    }

    private NotificationPriority mapToEntityPriority(NotificationPriority priority) {
        return priority != null ?
                NotificationPriority.valueOf(priority.name()) :
                NotificationPriority.NORMAL;
    }

    private NotificationStatus mapToEntityStatus(NotificationStatus status) {
        return NotificationStatus.valueOf(status.name());
    }

    private NotificationType mapToTemplateType(NotificationType type) {
        return NotificationType.valueOf(type.name());
    }

    private String getRecipientAddress(NotificationChannel channel,
                                       CustomerContactEntity contact) {
        return switch (channel) {
            case EMAIL -> contact.getEmail();
            case SMS -> contact.getPhoneNumber();
            case WEBSOCKET -> contact.getCustomerId();
            default -> null;
        };
    }
}