package com.bankingplatform.notificationservice.service.channels;

import com.bankingplatform.notificationservice.entity.CustomerContactEntity;
import com.bankingplatform.notificationservice.entity.NotificationEntity;
import com.bankingplatform.notificationservice.model.NotificationResult;
import com.bankingplatform.notificationservice.model.NotificationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${sms.provider.url:http://localhost:8999/sms/send}")
    private String smsProviderUrl;

    @Value("${sms.provider.api-key:demo-key}")
    private String apiKey;

    @Value("${sms.enabled:false}")
    private boolean smsEnabled;

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public NotificationResult sendSms(NotificationEntity notification, CustomerContactEntity contact) {
        try {
            log.info("Sending SMS to: {} for notification: {}", contact.getPhoneNumber(), notification.getId());

            if (!smsEnabled) {
                log.info("SMS is disabled, simulating SMS send to: {}", contact.getPhoneNumber());
                return simulateSmsResult(notification);
            }

            // Prepare SMS request
            Map<String, Object> smsRequest = Map.of(
                    "to", contact.getPhoneNumber(),
                    "message", notification.getMessage(),
                    "from", "BankingPlatform"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(smsRequest, headers);

            // Send SMS via external provider
            Map<String, Object> response = restTemplate.exchange(
                    smsProviderUrl,
                    HttpMethod.POST,
                    request,
                    Map.class
            ).getBody();

            String externalId = response != null ?
                    response.get("messageId").toString() :
                    "SMS-" + UUID.randomUUID().toString().substring(0, 8);

            log.info("Successfully sent SMS to: {} with ID: {}", contact.getPhoneNumber(), externalId);

            return NotificationResult.builder()
                    .notificationId(notification.getId())
                    .status(NotificationStatus.SENT)
                    .message("SMS sent successfully")
                    .sentAt(LocalDateTime.now())
                    .externalId(externalId)
                    .build();

        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", contact.getPhoneNumber(), e.getMessage(), e);
            return NotificationResult.builder()
                    .notificationId(notification.getId())
                    .status(NotificationStatus.FAILED)
                    .message("SMS sending failed: " + e.getMessage())
                    .sentAt(LocalDateTime.now())
                    .build();
        }
    }

    private NotificationResult simulateSmsResult(NotificationEntity notification) {
        // Simulate SMS sending for demo purposes
        String externalId = "DEMO-SMS-" + UUID.randomUUID().toString().substring(0, 8);

        return NotificationResult.builder()
                .notificationId(notification.getId())
                .status(NotificationStatus.SENT)
                .message("SMS sent successfully (simulated)")
                .sentAt(LocalDateTime.now())
                .externalId(externalId)
                .build();
    }
}