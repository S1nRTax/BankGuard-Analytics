package com.bankingplatform.streamprocessor.topology;

import com.bankingplatform.streamprocessor.model.Transaction;
import com.bankingplatform.streamprocessor.service.TransactionProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.kafka.support.serializer.JsonSerde;

@Slf4j
@Component
@RequiredArgsConstructor
public class StreamProcessorTopology {

    private final TransactionProcessingService processingService;
    private final JsonSerde<Transaction> transactionSerde;

    @Autowired
    public void buildPipeline(StreamsBuilder streamsBuilder) {
        log.info("Building Kafka Streams topology...");

        KStream<String, Transaction> transactionStream = streamsBuilder
                .stream("banking-transactions",
                        Consumed.with(Serdes.String(), transactionSerde))
                .peek((key, transaction) -> {
                    if (transaction != null) {
                        log.debug("Received transaction: {} for customer: {}",
                                transaction.getTransactionId(), transaction.getCustomerId());
                    }
                });

        // Process transactions
        transactionStream
                .filter((key, transaction) -> transaction != null)
                .foreach((key, transaction) -> {
                    try {
                        processingService.processTransaction(transaction);
                        log.debug("Processed transaction: {}", transaction.getTransactionId());
                    } catch (Exception e) {
                        log.error("Error processing transaction {}: {}",
                                transaction.getTransactionId(), e.getMessage(), e);
                    }
                });

        log.info("Kafka Streams topology built successfully");
    }
}
