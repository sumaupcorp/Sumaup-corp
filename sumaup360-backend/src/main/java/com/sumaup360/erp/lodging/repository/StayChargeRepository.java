package com.sumaup360.erp.lodging.repository;

import com.sumaup360.erp.lodging.domain.StayCharge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StayChargeRepository extends JpaRepository<StayCharge, UUID> {

    Optional<StayCharge> findByIdAndTenantId(UUID id, UUID tenantId);

    List<StayCharge> findByTenantIdAndStayIdOrderByCreatedAtAsc(UUID tenantId, UUID stayId);
}
