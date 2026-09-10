package com.sumaup360.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/** Modulo por defecto de un rubro (catalogo global de configuracion). */
@Entity
@Table(name = "business_type_module", schema = "catalog")
@Getter
public class BusinessTypeModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_type_code", nullable = false)
    private String businessTypeCode;

    @Column(name = "module_code", nullable = false)
    private String moduleCode;
}
