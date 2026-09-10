package com.sumaup360.app.aidiagnosis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DiagnosisReportRepository extends JpaRepository<DiagnosisReport, UUID> {
    List<DiagnosisReport> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
