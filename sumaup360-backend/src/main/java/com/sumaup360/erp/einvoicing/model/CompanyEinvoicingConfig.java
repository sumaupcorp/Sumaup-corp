package com.sumaup360.erp.einvoicing.model;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Credenciales de facturacion electronica de una empresa (RUC emisor).
 * NubeFact entrega una ruta (URL unica) + token; el token se guarda CIFRADO.
 */
@Entity
@Table(name = "company_einvoicing_config", schema = "erp")
@Getter
@Setter
public class CompanyEinvoicingConfig extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "provider", nullable = false, length = 20)
    private String provider = "NUBEFACT";

    @Column(name = "nubefact_ruta", length = 500)
    private String nubefactRuta;

    /** Token de NubeFact cifrado con CryptoService (nunca en claro en BD). */
    @Column(name = "nubefact_token_enc", length = 1000)
    private String nubefactTokenEnc;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = false;
}
