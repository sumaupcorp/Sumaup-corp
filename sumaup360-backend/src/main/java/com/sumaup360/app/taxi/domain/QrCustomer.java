package com.sumaup360.app.taxi.domain;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Cliente final del QR (Linea Personas). GLOBAL: reutilizable entre taxistas; se busca
 * por documento para precargar sus datos. Distinto del Customer del ERP (Negocios).
 */
@Entity(name = "QrCustomer")
@Table(name = "customer", schema = "app")
@Getter
@Setter
public class QrCustomer extends BaseEntity {

    @Column(name = "doc_type", length = 10)
    private String docType; // DNI | RUC

    @Column(name = "doc_number", length = 15)
    private String docNumber;

    @Column(name = "name", length = 200)
    private String name;

    @Column(name = "whatsapp", length = 30)
    private String whatsapp;

    @Column(name = "email", length = 160)
    private String email;
}
