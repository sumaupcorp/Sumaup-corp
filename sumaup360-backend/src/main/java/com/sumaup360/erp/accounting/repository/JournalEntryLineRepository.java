package com.sumaup360.erp.accounting.repository;

import com.sumaup360.erp.accounting.model.JournalEntryLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JournalEntryLineRepository extends JpaRepository<JournalEntryLine, UUID> {

    List<JournalEntryLine> findByJournalEntryIdOrderByOrdenAsc(UUID journalEntryId);
}
