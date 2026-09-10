package com.sumaup360.app.repository;

import com.sumaup360.app.domain.Income;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncomeRepository extends JpaRepository<Income, UUID> {

    List<Income> findByUserIdOrderByTxDateDesc(UUID userId);

    Optional<Income> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
            SELECT COALESCE(SUM(i.amount), 0) FROM Income i
            WHERE i.userId = :userId AND i.txDate BETWEEN :from AND :to
            """)
    BigDecimal sumInRange(@Param("userId") UUID userId,
                          @Param("from") LocalDate from,
                          @Param("to") LocalDate to);
}
