package com.sumaup360.erp.lodging.repository;

import com.sumaup360.erp.lodging.domain.Stay;
import com.sumaup360.erp.lodging.enums.StayStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StayRepository extends JpaRepository<Stay, UUID> {

    Optional<Stay> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Stay> findByTenantIdAndTicketCode(UUID tenantId, String ticketCode);

    boolean existsByTenantIdAndTicketCode(UUID tenantId, String ticketCode);

    /** Estadias vivas (RESERVED/CHECKED_IN) que cruzan el rango [checkIn, checkOut) de la habitacion. */
    @Query("""
            SELECT COUNT(s) FROM Stay s
            WHERE s.tenantId = :tenantId
              AND s.roomId = :roomId
              AND s.status IN :statuses
              AND s.checkInDate < :checkOut
              AND s.checkOutDate > :checkIn
              AND (:excludeId IS NULL OR s.id <> :excludeId)
            """)
    long countOverlapping(@Param("tenantId") UUID tenantId,
                          @Param("roomId") UUID roomId,
                          @Param("statuses") Collection<StayStatus> statuses,
                          @Param("checkIn") LocalDate checkIn,
                          @Param("checkOut") LocalDate checkOut,
                          @Param("excludeId") UUID excludeId);

    List<Stay> findByTenantIdAndBranchIdAndStatusIn(UUID tenantId, UUID branchId,
                                                    Collection<StayStatus> statuses);

    boolean existsByTenantIdAndRoomIdAndStatus(UUID tenantId, UUID roomId, StayStatus status);

    /** Estadias vivas de la sucursal que cruzan el rango [from, to) — para disponibilidad. */
    List<Stay> findByTenantIdAndBranchIdAndStatusInAndCheckInDateLessThanAndCheckOutDateGreaterThan(
            UUID tenantId, UUID branchId, Collection<StayStatus> statuses,
            LocalDate to, LocalDate from);

    Page<Stay> findByTenantIdAndCompanyId(UUID tenantId, UUID companyId, Pageable pageable);

    Page<Stay> findByTenantIdAndCompanyIdAndStatus(UUID tenantId, UUID companyId,
                                                   StayStatus status, Pageable pageable);

    Page<Stay> findByTenantIdAndBranchId(UUID tenantId, UUID branchId, Pageable pageable);

    Page<Stay> findByTenantIdAndBranchIdAndStatus(UUID tenantId, UUID branchId,
                                                  StayStatus status, Pageable pageable);
}
