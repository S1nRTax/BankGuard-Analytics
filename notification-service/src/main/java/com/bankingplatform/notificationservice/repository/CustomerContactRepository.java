package com.bankingplatform.notificationservice.repository;

import com.bankingplatform.notificationservice.entity.CustomerContactEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerContactRepository extends JpaRepository<CustomerContactEntity, String> {

    Optional<CustomerContactEntity> findByCustomerId(String customerId);

    Optional<CustomerContactEntity> findByEmail(String email);

    Optional<CustomerContactEntity> findByPhoneNumber(String phoneNumber);

    @Query("SELECT c FROM CustomerContactEntity c WHERE c.emailEnabled = true " +
            "AND c.email IS NOT NULL AND c.email != ''")
    List<CustomerContactEntity> findAllWithEmailEnabled();

    @Query("SELECT c FROM CustomerContactEntity c WHERE c.smsEnabled = true " +
            "AND c.phoneNumber IS NOT NULL AND c.phoneNumber != ''")
    List<CustomerContactEntity> findAllWithSmsEnabled();

    @Query("SELECT c FROM CustomerContactEntity c WHERE c.fraudAlertsEnabled = true " +
            "AND c.customerId = :customerId")
    Optional<CustomerContactEntity> findByCustomerIdAndFraudAlertsEnabled(
            @Param("customerId") String customerId);

    @Query("SELECT COUNT(c) FROM CustomerContactEntity c WHERE c.emailEnabled = true")
    Long countEmailEnabledCustomers();

    @Query("SELECT COUNT(c) FROM CustomerContactEntity c WHERE c.smsEnabled = true")
    Long countSmsEnabledCustomers();
}