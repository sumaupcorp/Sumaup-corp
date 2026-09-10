package com.sumaup360.backoffice.repository;

import com.sumaup360.backoffice.domain.ClientHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClientHistoryRepository extends JpaRepository<ClientHistory, UUID> {
    List<ClientHistory> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
