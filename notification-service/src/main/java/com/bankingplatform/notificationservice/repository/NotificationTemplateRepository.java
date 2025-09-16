package com.bankingplatform.notificationservice.repository;

import com.bankingplatform.notificationservice.entity.NotificationTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.bankingplatform.notificationservice.model.NotificationType;
import com.bankingplatform.notificationservice.model.NotificationChannel;


import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplateEntity, String> {

    Optional<NotificationTemplateEntity> findByTemplateId(String templateId);

    @Query("SELECT t FROM NotificationTemplateEntity t WHERE t.type = :type " +
            "AND t.channel = :channel AND t.language = :language AND t.isActive = true")
    Optional<NotificationTemplateEntity> findByTypeAndChannelAndLanguage(
            @Param("type")  NotificationType type,
            @Param("channel") NotificationChannel channel,
            @Param("language") String language);

    @Query("SELECT t FROM NotificationTemplateEntity t WHERE t.type = :type " +
            "AND t.channel = :channel AND t.isActive = true")
    List<NotificationTemplateEntity> findByTypeAndChannel(
            @Param("type") NotificationType type,
            @Param("channel") NotificationChannel channel);

    List<NotificationTemplateEntity> findByTypeAndIsActive(
            NotificationType type, Boolean isActive);

    @Query("SELECT DISTINCT t.language FROM NotificationTemplateEntity t WHERE t.isActive = true")
    List<String> findAllActiveLanguages();
}