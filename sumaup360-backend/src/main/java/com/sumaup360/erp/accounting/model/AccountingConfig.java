package com.sumaup360.erp.accounting.model;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Configuracion contable por empresa: cuentas por defecto, subdiarios y formato CONCAR. */
@Entity
@Table(name = "accounting_config", schema = "erp")
@Getter
@Setter
public class AccountingConfig extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "subdiario_ventas", nullable = false, length = 4)
    private String subdiarioVentas = "14";

    @Column(name = "subdiario_compras", nullable = false, length = 4)
    private String subdiarioCompras = "02";

    @Column(name = "subdiario_diario", nullable = false, length = 4)
    private String subdiarioDiario = "01";

    @Column(name = "cuenta_por_cobrar", nullable = false, length = 20)
    private String cuentaPorCobrar = "1212";

    @Column(name = "cuenta_ventas", nullable = false, length = 20)
    private String cuentaVentas = "7011";

    @Column(name = "cuenta_ventas_serv", nullable = false, length = 20)
    private String cuentaVentasServ = "7041";

    @Column(name = "cuenta_igv", nullable = false, length = 20)
    private String cuentaIgv = "40111";

    @Column(name = "cuenta_caja", nullable = false, length = 20)
    private String cuentaCaja = "101";

    @Column(name = "moneda", nullable = false, length = 2)
    private String moneda = "MN";

    @Column(name = "concar_separator", nullable = false, length = 4)
    private String concarSeparator = "|";
}
