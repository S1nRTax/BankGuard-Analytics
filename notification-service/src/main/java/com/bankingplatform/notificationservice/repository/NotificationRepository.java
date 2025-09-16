package com.bankingplatform.notificationservice.repository;

import com.bankingplatform.notificationservice.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, String> {

    List<NotificationEntity> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<NotificationEntity> findByStatusOrderByCreatedAtAsc(NotificationEntity.NotificationStatus status);

    List<NotificationEntity> findByStatusAndPriorityOrderByCreatedAtAsc(
            NotificationEntity.NotificationStatus status,
            NotificationEntity.NotificationPriority priority);

    @Query("SELECT n FROM NotificationEntity n WHERE n.scheduledTime <= :now " +
            "AND n.status = 'PENDING' ORDER BY n.priority DESC, n.scheduledTime ASC")
    List<NotificationEntity> findReadyToSend(@Param("now") LocalDateTime now);

    @Query("SELECT n FROM NotificationEntity n WHERE n.status = 'RETRYING' " +
            "AND n.retryCount < 3 ORDER BY n.createdAt ASC")
    List<NotificationEntity> findFailedNotificationsToRetry();

    @Query("SELECT COUNT(n) FROM NotificationEntity n WHERE n.customerId = :customerId " +
            "AND n.createdAt >= :since")
    Long countByCustomerIdAndCreatedAtAfter(
            @Param("customerId") String customerId,
            @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(n) FROM NotificationEntity n WHERE n.status = :status " +
            "AND n.createdAt >= :since")
    Long countByStatusAndCreatedAtAfter(
            @Param("status") NotificationEntity.NotificationStatus status,
            @Param("since") LocalDateTime since);

    Page<NotificationEntity> findByCustomerIdOrderByCreatedAtDesc(
            String customerId, Pageable pageable);

    List<NotificationEntity> findByTypeAndCreatedAtBetween(
            NotificationEntity.NotificationType type,
            LocalDateTime start,
            LocalDateTime end);

    @Query("SELECT n.status, COUNT(n) FROM NotificationEntity n " +
            "WHERE n.createdAt >= :since GROUP BY n.status")
    List<Object[]> getNotificationStatisticsSince(@Param("since") LocalDateTime since);

    @Query("SELECT n.type, COUNT(n) FROM NotificationEntity n " +
            "WHERE n.createdAt >= :since GROUP BY n.type")
    List<Object[]> getNotificationTypeStatisticsSince(@Param("since") LocalDateTime since);
}