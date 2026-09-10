package com.sumaup360.catalog.domain;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Producto del catalogo maestro: lo registra el staff (backoffice) por rubro y los tenants
 * del SaaS lo buscan para añadirlo a su inventario. Una sola foto por producto (los tenants
 * la heredan por referencia, nunca la duplican). photo_source/photo_source_url documentan la
 * procedencia de la imagen (fabricante, openfoodfacts, staff, tenant) para poder atender
 * cualquier reclamo puntual sin comprometer el catalogo.
 */
@Entity
@Table(name = "master_product", schema = "catalog")
@Getter
@Setter
public class MasterProduct extends BaseEntity {

    @Column(name = "ean", length = 20)
    private String ean;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "brand", length = 80)
    private String brand;

    @Column(name = "category", length = 80)
    private String category;

    @Column(name = "presentation", length = 80)
    private String presentation;

    @Column(name = "photo_url", length = 600)
    private String photoUrl;

    /** Hotlink a imagen externa (fabricante/tienda); si se cae, el front usa photoUrl. */
    @Column(name = "photo_external_url", length = 600)
    private String photoExternalUrl;

    @Column(name = "photo_source", length = 20)
    private String photoSource;

    @Column(name = "photo_source_url", length = 600)
    private String photoSourceUrl;

    @Column(name = "verified", nullable = false)
    private boolean verified = false;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    /** Codigos de rubro (catalog.business_types) donde se ofrece el producto. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "master_product_rubro", schema = "catalog",
            joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "business_type", nullable = false, length = 40)
    private Set<String> rubros = new LinkedHashSet<>();
}
