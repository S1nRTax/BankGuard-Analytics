// CustomerContactEntity.java
package com.bankingplatform.notificationservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_contacts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerContactEntity {

    @Id
    private String customerId;

    private String email;
    private String phoneNumber;
    private String pushToken;

    @Builder.Default
    private String preferredLanguage = "en";

    @Builder.Default
    private String timezone = "Africa/Casablanca";

    // Notification preferences
    @Builder.Default
    private Boolean emailEnabled = true;

    @Builder.Default
    private Boolean smsEnabled = true;

    @Builder.Default
    private Boolean pushEnabled = true;

    // Specific preferences
    @Builder.Default
    private Boolean fraudAlertsEnabled = true;

    @Builder.Default
    private Boolean transactionAlertsEnabled = true;

    @Builder.Default
    private Boolean marketingEnabled = false;

    private String alternateEmail;
    private String alternatePhone;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}