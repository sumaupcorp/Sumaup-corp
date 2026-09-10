package com.sumaup360.erp.appointment.domain;

import com.sumaup360.common.BaseEntity;
import com.sumaup360.erp.appointment.enums.AppointmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Cita por sucursal (modulo appointments: veterinaria, salon de belleza, etc.). */
@Entity
@Table(name = "appointment", schema = "erp")
@Getter
@Setter
public class Appointment extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "patient_id")
    private UUID patientId;

    @Column(name = "reason", length = 160)
    private String reason;

    @Column(name = "scheduled_at", nullable = false)
    private OffsetDateTime scheduledAt;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 30;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "notes", length = 500)
    private String notes;

    /** Respuestas del formulario dinamico (JSON) cuando la cita nace de la reserva online. */
    @Column(name = "form_data")
    private String formData;

    @Column(name = "source", nullable = false, length = 20)
    private String source = "INTERNAL";

    /** Codigo corto para que el negocio ubique la cita rapido (unico por tenant). */
    @Column(name = "ticket_code", length = 10)
    private String ticketCode;
}
