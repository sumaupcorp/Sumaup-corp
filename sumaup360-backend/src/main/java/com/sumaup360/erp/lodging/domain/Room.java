package com.sumaup360.erp.lodging.domain;

import com.sumaup360.common.BaseEntity;
import com.sumaup360.erp.lodging.enums.RoomStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Habitacion fisica por sucursal. El numero es unico dentro de la sucursal. */
@Entity
@Table(name = "room", schema = "erp")
@Getter
@Setter
public class Room extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "room_type_id", nullable = false)
    private UUID roomTypeId;

    @Column(name = "number", nullable = false, length = 20)
    private String number;

    @Column(name = "floor", length = 20)
    private String floor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RoomStatus status = RoomStatus.AVAILABLE;

    @Column(name = "notes", length = 300)
    private String notes;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
