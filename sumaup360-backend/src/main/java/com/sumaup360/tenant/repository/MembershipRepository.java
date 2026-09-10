package com.sumaup360.tenant.repository;

import com.sumaup360.tenant.domain.Membership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    List<Membership> findByUserId(UUID userId);

    List<Membership> findByTenantId(UUID tenantId);

    Optional<Membership> findByUserIdAndDefaultTenantTrue(UUID userId);

    boolean existsByUserIdAndTenantId(UUID userId, UUID tenantId);

    Optional<Membership> findByUserIdAndTenantId(UUID userId, UUID tenantId);
}
