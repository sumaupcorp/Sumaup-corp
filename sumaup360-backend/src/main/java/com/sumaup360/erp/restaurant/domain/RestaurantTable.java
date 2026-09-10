package com.sumaup360.erp.restaurant.domain;

import com.sumaup360.common.BaseEntity;
import com.sumaup360.erp.restaurant.enums.TableStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Mesa de un restaurante (por sucursal). */
@Entity
@Table(name = "restaurant_table", schema = "erp")
@Getter
@Setter
public class RestaurantTable extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @Column(name = "zone", length = 80)
    private String zone;

    @Column(name = "capacity", nullable = false)
    private int capacity = 4;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TableStatus status = TableStatus.FREE;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
