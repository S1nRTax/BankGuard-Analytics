package com.bankingplatform.notificationservice.model;

public enum NotificationPriority {
    LOW,    // Marketing, daily summaries
    NORMAL, // Transaction confirmations
    HIGH,   // Account changes
    URGENT  // Fraud alerts, security issues
}