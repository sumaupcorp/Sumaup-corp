package com.sumaup360.erp.patient.domain;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Paciente / mascota (modulo patients, p. ej. veterinaria). El dueno es un cliente del negocio. */
@Entity
@Table(name = "patient", schema = "erp")
@Getter
@Setter
public class Patient extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "species", length = 40)
    private String species;

    @Column(name = "breed", length = 60)
    private String breed;

    @Column(name = "sex", length = 10)
    private String sex;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "weight_kg", precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "notes", length = 500)
    private String notes;

    /** Foto de la mascota (URL en Firebase Storage). */
    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
