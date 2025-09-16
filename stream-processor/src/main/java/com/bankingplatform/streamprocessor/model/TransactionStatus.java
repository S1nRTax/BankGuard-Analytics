package com.bankingplatform.streamprocessor.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum TransactionStatus {
    @JsonProperty("PENDING") PENDING,
    @JsonProperty("COMPLETED") COMPLETED,
    @JsonProperty("FAILED") FAILED,
    @JsonProperty("CANCELLED") CANCELLED,
    @JsonProperty("PROCESSING") PROCESSING
}
