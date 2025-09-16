package com.bankingplatform.notificationservice.model;

public enum FraudReason {
    HIGH_AMOUNT,
    FREQUENT_TRANSACTIONS,
    UNUSUAL_LOCATION,
    HIGH_RISK_SCORE,
    VELOCITY_CHECK_FAILED,
    SUSPICIOUS_PATTERN
}