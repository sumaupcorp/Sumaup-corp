package com.sumaup360.erp.lodging.domain;

import com.sumaup360.common.BaseEntity;
import com.sumaup360.erp.lodging.enums.RentalMode;
import com.sumaup360.erp.lodging.enums.StayStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Estadia / reserva de una habitacion por rango de noches. check_out_date es
 * exclusiva: noches = check_out_date - check_in_date. El solape por habitacion
 * (estados vivos RESERVED/CHECKED_IN) se valida en el service.
 */
@Entity
@Table(name = "stay", schema = "erp")
@Getter
@Setter
public class Stay extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StayStatus status = StayStatus.RESERVED;

    /** NIGHTLY cobra noches; HOURLY entra y sale el mismo dia (no bloquea fechas). */
    @Enumerated(EnumType.STRING)
    @Column(name = "rental_mode", nullable = false, length = 10)
    private RentalMode rentalMode = RentalMode.NIGHTLY;

    /** Horas pactadas (solo HOURLY). */
    @Column(name = "hours")
    private Integer hours;

    /** Tarifa pactada por unidad: por noche (NIGHTLY) o por hora (HOURLY). */
    @Column(name = "rate_per_night", nullable = false, precision = 10, scale = 2)
    private BigDecimal ratePerNight = BigDecimal.ZERO;

    @Column(name = "guests_count", nullable = false)
    private int guestsCount = 1;

    @Column(name = "checked_in_at")
    private OffsetDateTime checkedInAt;

    @Column(name = "checked_out_at")
    private OffsetDateTime checkedOutAt;

    /** Venta POS generada en el check-out (noches + cargos). */
    @Column(name = "sale_id")
    private UUID saleId;

    /** Codigo corto para ubicar la reserva rapido (unico por tenant). */
    @Column(name = "ticket_code", length = 10)
    private String ticketCode;

    @Column(name = "source", nullable = false, length = 20)
    private String source = "INTERNAL";

    @Column(name = "notes", length = 500)
    private String notes;
}
