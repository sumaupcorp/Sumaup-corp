package com.sumaup360.erp.lodging.repository;

import com.sumaup360.erp.lodging.domain.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID> {

    Optional<Room> findByIdAndTenantId(UUID id, UUID tenantId);

    List<Room> findByTenantIdAndCompanyIdOrderByNumberAsc(UUID tenantId, UUID companyId);

    List<Room> findByTenantIdAndBranchIdOrderByNumberAsc(UUID tenantId, UUID branchId);

    boolean existsByTenantIdAndBranchIdAndNumberIgnoreCase(UUID tenantId, UUID branchId, String number);
}
