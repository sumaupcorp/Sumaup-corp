package com.sumaup360.erp.appointment.repository;

import com.sumaup360.erp.appointment.domain.Appointment;
import com.sumaup360.erp.appointment.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    Optional<Appointment> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndBranchIdAndCustomerIdAndStatus(
            UUID tenantId, UUID branchId, UUID customerId, AppointmentStatus status);

    Optional<Appointment> findByTenantIdAndTicketCode(UUID tenantId, String ticketCode);

    boolean existsByTenantIdAndTicketCode(UUID tenantId, String ticketCode);

    List<Appointment> findByTenantIdAndBranchIdAndScheduledAtBetweenOrderByScheduledAtAsc(
            UUID tenantId, UUID branchId, OffsetDateTime from, OffsetDateTime to);

    List<Appointment> findByTenantIdAndScheduledAtBetweenOrderByScheduledAtAsc(
            UUID tenantId, OffsetDateTime from, OffsetDateTime to);

    /** Historial de citas de un cliente (para su ficha y estadisticas). */
    List<Appointment> findByTenantIdAndCustomerIdOrderByScheduledAtDesc(UUID tenantId, UUID customerId);
}
