package com.sumaup360.erp.prescription.repository;

import com.sumaup360.erp.prescription.domain.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {

    Optional<Prescription> findByIdAndTenantId(UUID id, UUID tenantId);

    List<Prescription> findByTenantIdOrderByIssuedDateDesc(UUID tenantId);

    List<Prescription> findByTenantIdAndBranchIdOrderByIssuedDateDesc(UUID tenantId, UUID branchId);
}
