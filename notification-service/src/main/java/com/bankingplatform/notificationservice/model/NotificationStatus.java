package com.bankingplatform.notificationservice.model;

public enum NotificationStatus {
    PENDING,
    SENT,
    DELIVERED,
    FAILED,
    RETRYING,
    CANCELLED
}