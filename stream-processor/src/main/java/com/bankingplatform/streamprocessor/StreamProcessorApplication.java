// StreamProcessorApplication.java
package com.bankingplatform.streamprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableKafkaStreams
@EnableScheduling
@EnableTransactionManagement
public class StreamProcessorApplication {

	public static void main(String[] args) {
		SpringApplication.run(StreamProcessorApplication.class, args);
	}
}