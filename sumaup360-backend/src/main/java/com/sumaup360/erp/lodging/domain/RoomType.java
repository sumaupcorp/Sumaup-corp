package com.sumaup360.erp.lodging.domain;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/** Tipo de habitacion por empresa (Simple, Doble, Matrimonial, Suite...). */
@Entity
@Table(name = "room_type", schema = "erp")
@Getter
@Setter
public class RoomType extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "capacity", nullable = false)
    private int capacity = 2;

    @Column(name = "rate_per_night", nullable = false, precision = 10, scale = 2)
    private BigDecimal ratePerNight = BigDecimal.ZERO;

    /** Tarifa por hora; null = este tipo no se alquila por horas. */
    @Column(name = "rate_per_hour", precision = 10, scale = 2)
    private BigDecimal ratePerHour;

    @Column(name = "description", length = 300)
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
