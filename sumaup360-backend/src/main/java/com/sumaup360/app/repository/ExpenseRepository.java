package com.sumaup360.app.repository;

import com.sumaup360.app.domain.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    List<Expense> findByUserIdOrderByTxDateDesc(UUID userId);

    Optional<Expense> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
            SELECT COALESCE(SUM(e.amount), 0) FROM Expense e
            WHERE e.userId = :userId AND e.txDate BETWEEN :from AND :to
            """)
    BigDecimal sumInRange(@Param("userId") UUID userId,
                          @Param("from") LocalDate from,
                          @Param("to") LocalDate to);
}
