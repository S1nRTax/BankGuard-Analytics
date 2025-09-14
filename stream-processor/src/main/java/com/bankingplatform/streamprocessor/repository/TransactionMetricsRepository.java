package com.bankingplatform.streamprocessor.repository;

import com.bankingplatform.streamprocessor.entity.TransactionMetricsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionMetricsRepository extends JpaRepository<TransactionMetricsEntity, Long> {

    Optional<TransactionMetricsEntity> findByWindowStart(LocalDateTime windowStart);

    List<TransactionMetricsEntity> findByWindowStartBetweenOrderByWindowStart(
            LocalDateTime start, LocalDateTime end);

    @Query("SELECT t FROM TransactionMetricsEntity t WHERE t.windowStart >= :since " +
            "ORDER BY t.windowStart DESC")
    List<TransactionMetricsEntity> findRecentMetrics(@Param("since") LocalDateTime since);

    @Query("SELECT MAX(t.windowStart) FROM TransactionMetricsEntity t")
    Optional<LocalDateTime> findLatestWindowStart();
}