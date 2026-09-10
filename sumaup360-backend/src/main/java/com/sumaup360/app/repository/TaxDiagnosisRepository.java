package com.sumaup360.app.repository;

import com.sumaup360.app.domain.TaxDiagnosis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TaxDiagnosisRepository extends JpaRepository<TaxDiagnosis, UUID> {
    Optional<TaxDiagnosis> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);
}
