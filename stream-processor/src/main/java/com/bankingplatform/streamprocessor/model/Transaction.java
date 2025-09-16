package com.bankingplatform.streamprocessor.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    private String transactionId;
    private String customerId;
    private String accountNumber;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private TransactionType type;

    private BigDecimal amount;
    private String currency;
    private String merchantName;
    private String merchantCategory;
    private String description;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
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
