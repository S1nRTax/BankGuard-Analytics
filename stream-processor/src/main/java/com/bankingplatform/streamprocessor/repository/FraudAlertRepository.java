package com.bankingplatform.streamprocessor.repository;

import com.bankingplatform.streamprocessor.entity.FraudAlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlertEntity, String> {

    List<FraudAlertEntity> findByCustomerIdOrderByTimestampDesc(String customerId);

    List<FraudAlertEntity> findByStatusOrderByTimestampDesc(
            FraudAlertEntity.AlertStatus status);

    @Query("SELECT COUNT(f) FROM FraudAlertEntity f WHERE f.customerId = :customerId " +
            "AND f.timestamp >= :since")
    Long countByCustomerIdAndTimestampAfter(
            @Param("customerId") String customerId,
            @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(f) FROM FraudAlertEntity f WHERE f.timestamp >= :since")
    Long countAlertsSince(@Param("since") LocalDateTime since);

    List<FraudAlertEntity> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT f.reason, COUNT(f) FROM FraudAlertEntity f " +
            "WHERE f.timestamp >= :since GROUP BY f.reason ORDER BY COUNT(f) DESC")
    List<Object[]> findAlertReasonStatisticsSince(@Param("since") LocalDateTime since);
}