package com.sumaup360.erp.repository;

import com.sumaup360.erp.domain.CashMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CashMovementRepository extends JpaRepository<CashMovement, UUID> {
    List<CashMovement> findByCashSessionIdOrderByCreatedAtDesc(UUID cashSessionId);
}
