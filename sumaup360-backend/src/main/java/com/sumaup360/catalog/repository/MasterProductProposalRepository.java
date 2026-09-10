package com.sumaup360.catalog.repository;

import com.sumaup360.catalog.domain.MasterProductProposal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MasterProductProposalRepository extends JpaRepository<MasterProductProposal, UUID> {

    List<MasterProductProposal> findByStatusOrderByCreatedAtAsc(String status);

    long countByStatus(String status);

    boolean existsByTenantIdAndNameIgnoreCaseAndStatus(UUID tenantId, String name, String status);
}
