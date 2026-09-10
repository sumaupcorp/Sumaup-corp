package com.sumaup360.erp.lodging.repository;

import com.sumaup360.erp.lodging.domain.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomTypeRepository extends JpaRepository<RoomType, UUID> {

    Optional<RoomType> findByIdAndTenantId(UUID id, UUID tenantId);

    List<RoomType> findByTenantIdAndCompanyIdOrderByNameAsc(UUID tenantId, UUID companyId);
}
