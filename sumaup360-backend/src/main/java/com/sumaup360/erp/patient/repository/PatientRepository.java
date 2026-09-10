package com.sumaup360.erp.patient.repository;

import com.sumaup360.erp.patient.domain.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByIdAndTenantId(UUID id, UUID tenantId);

    List<Patient> findByTenantIdOrderByNameAsc(UUID tenantId);

    List<Patient> findByTenantIdAndCustomerIdOrderByNameAsc(UUID tenantId, UUID customerId);
}
