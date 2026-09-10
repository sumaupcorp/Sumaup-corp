package com.sumaup360.catalog.domain;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Propuesta de un tenant al catalogo maestro: nace automaticamente cuando crea un
 * producto propio que no existe en el catalogo. El staff la aprueba (se crea el
 * master_product y se vincula el producto original) o la rechaza.
 */
@Entity
@Table(name = "master_product_proposal", schema = "catalog")
@Getter
@Setter
public class MasterProductProposal extends BaseEntity {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String APROBADA = "APROBADA";
    public static final String RECHAZADA = "RECHAZADA";

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "company_id")
    private UUID companyId;

    /** Producto erp.product del tenant que origino la propuesta (se vincula al aprobar). */
    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "business_type", length = 40)
    private String businessType;

    @Column(name = "ean", length = 20)
    private String ean;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "category", length = 80)
    private String category;

    @Column(name = "status", nullable = false, length = 15)
    private String status = PENDIENTE;
}
