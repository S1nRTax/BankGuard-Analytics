package com.bankingplatform.streamprocessor.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TransactionType {
    PAYMENT, TRANSFER, WITHDRAWAL, DEPOSIT, REFUND, SUBSCRIPTION, INVESTMENT;

    @JsonCreator
    public static TransactionType fromValue(String value) {
        return TransactionType.valueOf(value.toUpperCase());
    }
}