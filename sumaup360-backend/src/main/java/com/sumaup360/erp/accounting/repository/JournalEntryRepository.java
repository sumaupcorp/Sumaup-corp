package com.sumaup360.erp.accounting.repository;

import com.sumaup360.erp.accounting.model.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {

    Optional<JournalEntry> findByIdAndTenantId(UUID id, UUID tenantId);

    List<JournalEntry> findByTenantIdAndCompanyIdAndEntryDateBetweenOrderByEntryDateAscCreatedAtAsc(
            UUID tenantId, UUID companyId, LocalDate from, LocalDate to);

    long countByTenantIdAndCompanyIdAndSubdiario(UUID tenantId, UUID companyId, String subdiario);

    boolean existsByTenantIdAndSourceAndSourceId(UUID tenantId, String source, UUID sourceId);
}
