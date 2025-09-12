package com.bankingplatform.transactiongenerator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;


@Data
@Configuration
@ConfigurationProperties(prefix = "transaction.generator")
public class TransactionGeneratorConfig {
    private int intervalMs = 1000; // Generate every 1000ms -> every 1s
    private int batchSize = 5; // Generate 5 transaction per batch
    private boolean enabled = true;

    private List<String> currencies = List.of("MAD", "EUR", "USD");
    private String defaultCurrency = "MAD";


    private Map<String, Integer> typeDistribution = Map.of(
            "PAYMENT", 60,     // 60% payments
            "TRANSFER", 20,    // 20% transfers
            "WITHDRAWAL", 15,  // 15% withdrawals
            "DEPOSIT", 5       // 5% deposits
    );

    private List<String> moroccanMerchants = List.of(
            "Marjane", "Carrefour", "Aswak Assalam", "Label Vie",
            "BMCE Bank", "Attijariwafa Bank", "Shell Morocco",
            "Meditel", "Orange Morocco", "ONCF"
    );

    private List<String> merchantCategories = List.of(
            "GROCERY", "FUEL", "RESTAURANT", "RETAIL", "TELECOM",
            "TRANSPORT", "HEALTHCARE", "ENTERTAINMENT", "UTILITIES"
    );

    private List<String> moroccanCities = List.of(
            "Casablanca", "Rabat", "Marrakech", "Fez", "Tangier",
            "Agadir", "Meknes", "Oujda", "Kenitra", "Tetouan"
    );


}
