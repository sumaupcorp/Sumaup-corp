package com.sumaup360.erp.prescription.domain;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/** Receta medica registrada por la farmacia (modulo prescription, MVP sin flujo de dispensacion). */
@Entity
@Table(name = "prescription", schema = "erp")
@Getter
@Setter
public class Prescription extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "sale_id")
    private UUID saleId;

    @Column(name = "doctor_name", nullable = false, length = 120)
    private String doctorName;

    @Column(name = "doctor_license", length = 20)
    private String doctorLicense;

    @Column(name = "issued_date", nullable = false)
    private LocalDate issuedDate;

    @Column(name = "diagnosis", length = 200)
    private String diagnosis;

    @Column(name = "medications", nullable = false)
    private String medications;

    @Column(name = "notes", length = 300)
    private String notes;
}
