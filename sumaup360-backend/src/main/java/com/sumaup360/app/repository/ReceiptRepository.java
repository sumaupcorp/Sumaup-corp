package com.sumaup360.app.repository;

import com.sumaup360.app.domain.Receipt;
import com.sumaup360.app.enums.ReceiptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {
    List<Receipt> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<Receipt> findByIdAndUserId(UUID id, UUID userId);

    /** Backoffice (cross-user): recibos por estado para la cola de intake. */
    List<Receipt> findByStatusOrderByCreatedAtAsc(ReceiptStatus status);

    long countByStatus(ReceiptStatus status);

    /** Usuarios (distintos) con recibos en un estado; para recordatorios de push. */
    @Query("select distinct r.userId from Receipt r where r.status = :status")
    List<UUID> findDistinctUserIdsByStatus(@Param("status") ReceiptStatus status);
}
