package com.bankingplatform.notificationservice.service.channels;

import com.bankingplatform.notificationservice.entity.CustomerContactEntity;
import com.bankingplatform.notificationservice.entity.NotificationEntity;
import com.bankingplatform.notificationservice.model.NotificationResult;
import com.bankingplatform.notificationservice.model.NotificationStatus;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@bankingplatform.com}")
    private String fromEmail;

    @Retryable(value = {MailException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public NotificationResult sendEmail(NotificationEntity notification, CustomerContactEntity contact) {
        try {
            log.info("Sending email to: {} for notification: {}", contact.getEmail(), notification.getId());

            if (notification.getHtmlMessage() != null) {
                return sendHtmlEmail(notification, contact);
            } else {
                return sendSimpleEmail(notification, contact);
            }

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", contact.getEmail(), e.getMessage(), e);
            return NotificationResult.builder()
                    .notificationId(notification.getId())
                    .status(NotificationStatus.FAILED)
                    .message("Email sending failed: " + e.getMessage())
                    .sentAt(LocalDateTime.now())
                    .build();
        }
    }

    private NotificationResult sendSimpleEmail(NotificationEntity notification, CustomerContactEntity contact)
            throws MailException {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(contact.getEmail());
        message.setSubject(notification.getSubject());
        message.setText(notification.getMessage());

        mailSender.send(message);

        String externalId = "EMAIL-" + UUID.randomUUID().toString().substring(0, 8);

        log.info("Successfully sent simple email to: {} with ID: {}", contact.getEmail(), externalId);

        return NotificationResult.builder()
                .notificationId(notification.getId())
                .status(NotificationStatus.SENT)
                .message("Email sent successfully")
                .sentAt(LocalDateTime.now())
                .externalId(externalId)
                .build();
    }

    private NotificationResult sendHtmlEmail(NotificationEntity notification, CustomerContactEntity contact)
            throws MessagingException, jakarta.mail.MessagingException {

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(contact.getEmail());
        helper.setSubject(notification.getSubject());
        helper.setText(notification.getMessage(), notification.getHtmlMessage());

        mailSender.send(mimeMessage);

        String externalId = "HTML-EMAIL-" + UUID.randomUUID().toString().substring(0, 8);

        log.info("Successfully sent HTML email to: {} with ID: {}", contact.getEmail(), externalId);

        return NotificationResult.builder()
                .notificationId(notification.getId())
                .status(NotificationStatus.SENT)
                .message("HTML email sent successfully")
                .sentAt(LocalDateTime.now())
                .externalId(externalId)
                .build();
    }
}