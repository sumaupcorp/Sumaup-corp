package com.sumaup360.erp.lodging.repository;

import com.sumaup360.erp.lodging.domain.StayGuest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StayGuestRepository extends JpaRepository<StayGuest, UUID> {

    List<StayGuest> findByTenantIdAndStayIdOrderByCreatedAtAsc(UUID tenantId, UUID stayId);

    void deleteByTenantIdAndStayId(UUID tenantId, UUID stayId);
}
