package com.sumaup360.erp.domain;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/** Producto del catalogo de un negocio (tenant). SKU unico por tenant. */
@Entity
@Table(name = "product", schema = "erp")
@Getter
@Setter
public class Product extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** Empresa dueña del producto: los inventarios de empresas del mismo tenant NO se cruzan. */
    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "sku", nullable = false, length = 40)
    private String sku;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "unit", nullable = false, length = 20)
    private String unit = "UNIDAD";

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "category", length = 80)
    private String category;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /** Referencia al catalogo maestro (hereda nombre/foto por referencia, sin duplicar). */
    @Column(name = "master_product_id")
    private UUID masterProductId;

    /** Foto propia del negocio (Firebase Storage). Si existe, manda sobre la del maestro. */
    @Column(name = "photo_url", length = 500)
    private String photoUrl;
}
