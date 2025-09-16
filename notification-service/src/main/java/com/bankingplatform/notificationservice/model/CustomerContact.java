package com.bankingplatform.notificationservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerContact {
    private String customerId;
    private String email;
    private String phoneNumber;
    private String pushToken;
    private String preferredLanguage;
    private boolean emailEnabled;
    private boolean smsEnabled;
    private boolean pushEnabled;
    private String timezone;
}