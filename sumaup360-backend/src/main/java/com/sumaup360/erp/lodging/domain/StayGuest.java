package com.sumaup360.erp.lodging.domain;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Huesped registrado en una estadia (ficha obligatoria, DS 001-2015-MINCETUR). */
@Entity
@Table(name = "stay_guest", schema = "erp")
@Getter
@Setter
public class StayGuest extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "stay_id", nullable = false)
    private UUID stayId;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "doc_type", nullable = false, length = 10)
    private String docType = "DNI";

    @Column(name = "doc_number", nullable = false, length = 20)
    private String docNumber;

    @Column(name = "nationality", nullable = false, length = 60)
    private String nationality = "Peru";
}
