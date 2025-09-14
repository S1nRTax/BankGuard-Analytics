package com.bankingplatform.streamprocessor.repository;

import com.bankingplatform.streamprocessor.entity.CustomerSummaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CustomerSummaryRepository extends JpaRepository<CustomerSummaryEntity, String> {

    @Query("SELECT c FROM CustomerSummaryEntity c WHERE c.totalAmount > :minAmount " +
            "ORDER BY c.totalAmount DESC")
    List<CustomerSummaryEntity> findHighValueCustomers(@Param("minAmount") BigDecimal minAmount);

    @Query("SELECT c FROM CustomerSummaryEntity c WHERE c.avgRiskScore > :riskThreshold")
    List<CustomerSummaryEntity> findHighRiskCustomers(@Param("riskThreshold") Double riskThreshold);

    @Query("SELECT c FROM CustomerSummaryEntity c WHERE c.transactionsLast1Hour > :threshold")
    List<CustomerSummaryEntity> findCustomersWithHighActivity(@Param("threshold") Long threshold);

    @Query("SELECT AVG(c.avgAmount) FROM CustomerSummaryEntity c")
    BigDecimal findGlobalAverageTransactionAmount();
}