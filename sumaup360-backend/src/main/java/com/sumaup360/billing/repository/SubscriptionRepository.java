package com.sumaup360.billing.repository;

import com.sumaup360.billing.domain.Subscription;
import com.sumaup360.billing.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    List<Subscription> findByTenantId(UUID tenantId);
    List<Subscription> findByUserId(UUID userId);
    Optional<Subscription> findFirstByTenantIdAndStatus(UUID tenantId, SubscriptionStatus status);
    Optional<Subscription> findFirstByUserIdAndStatus(UUID userId, SubscriptionStatus status);
}
