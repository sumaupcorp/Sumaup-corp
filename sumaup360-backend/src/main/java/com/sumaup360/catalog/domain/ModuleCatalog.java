package com.sumaup360.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/** Catalogo de modulos del sistema (company, inventory, sale, pos, ...). is_core = base. */
@Entity
@Table(name = "modules", schema = "catalog")
@Getter
public class ModuleCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "is_core", nullable = false)
    private boolean core;

    @Column(name = "is_active", nullable = false)
    private boolean active;
}
