package com.bankingplatform.streamprocessor.repository;

import com.bankingplatform.streamprocessor.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {

    List<TransactionEntity> findByCustomerIdOrderByTimestampDesc(String customerId);

    List<TransactionEntity> findByCustomerIdAndTimestampBetween(
            String customerId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(t) FROM TransactionEntity t WHERE t.customerId = :customerId " +
            "AND t.timestamp >= :since")
    Long countByCustomerIdAndTimestampAfter(
            @Param("customerId") String customerId,
            @Param("since") LocalDateTime since);

    @Query("SELECT SUM(t.amount) FROM TransactionEntity t WHERE t.customerId = :customerId " +
            "AND t.timestamp >= :since AND t.status = 'COMPLETED'")
    BigDecimal sumAmountByCustomerIdAndTimestampAfter(
            @Param("customerId") String customerId,
            @Param("since") LocalDateTime since);

    @Query("SELECT t.merchantCategory, COUNT(t) as count FROM TransactionEntity t " +
            "WHERE t.customerId = :customerId GROUP BY t.merchantCategory " +
            "ORDER BY count DESC")
    List<Object[]> findMostFrequentMerchantCategoryByCustomerId(
            @Param("customerId") String customerId);

    @Query("SELECT AVG(t.riskScore) FROM TransactionEntity t WHERE t.customerId = :customerId")
    Double findAvgRiskScoreByCustomerId(@Param("customerId") String customerId);

    @Query("SELECT COUNT(t) FROM TransactionEntity t WHERE t.timestamp >= :since")
    Long countTransactionsSince(@Param("since") LocalDateTime since);

    @Query("SELECT SUM(t.amount) FROM TransactionEntity t WHERE t.timestamp >= :since " +
            "AND t.status = 'COMPLETED'")
    BigDecimal sumAmountSince(@Param("since") LocalDateTime since);
}
