package com.sumaup360.billing.repository;

import com.sumaup360.billing.domain.Plan;
import com.sumaup360.billing.enums.ProductLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanRepository extends JpaRepository<Plan, UUID> {
    Optional<Plan> findByCode(String code);
    List<Plan> findByActiveTrue();
    List<Plan> findByActiveTrueAndLine(ProductLine line);
}
