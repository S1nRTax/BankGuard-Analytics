package com.bankingplatform.transactiongenerator.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.bankingplatform.transactiongenerator.model.TransactionStatus;
import com.bankingplatform.transactiongenerator.model.TransactionType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private String transactionId;
    private String customerId;
    private String accountNumber;
    private TransactionType type;
    private BigDecimal amount;
    private String currency;
    private String merchantName;
    private String merchantCategory;
    private String description;
    private TransactionStatus status;
    private String sourceLocation;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    // Risk-related fields for fraud detection
    private String ipAddress;
    private String deviceId;
    private Boolean isInternational;
    private Double riskScore;
}
