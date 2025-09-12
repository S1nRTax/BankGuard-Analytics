package com.bankingplatform.transactiongenerator.service;

import com.bankingplatform.transactiongenerator.model.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;


@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionProducer {


    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.transactions:banking-transactions}")
    private String transactionTopic;

    public CompletableFuture<SendResult<String, Object>> sendTransaction(Transaction transaction) {
        log.debug("Sending transaction: {}", transaction.getTransactionId());

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                transactionTopic,
                transaction.getCustomerId(), // Use customerId as partition key
                transaction
        );

        future.whenComplete((result, failure) -> {
            if (failure != null) {
                log.error("Failed to send transaction {}: {}",
                        transaction.getTransactionId(), failure.getMessage());
            } else {
                log.debug("Successfully sent transaction {} to partition {} offset {}",
                        transaction.getTransactionId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });

        return future;
    }


}
