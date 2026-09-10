package com.sumaup360.erp.appointment.domain;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Pagina publica de reserva de citas de una empresa (QR -> web publica /reservas/{token}).
 * El negocio configura logo, textos y los campos del formulario (form_config JSON).
 */
@Entity
@Table(name = "booking_page", schema = "erp")
@Getter
@Setter
public class BookingPage extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "token", nullable = false, length = 60, unique = true)
    private String token;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = false;

    @Column(name = "title", length = 120)
    private String title;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "welcome_text", length = 300)
    private String welcomeText;

    /** JSON con los campos del formulario: [{key,label,type,required}, ...]. */
    @Column(name = "form_config", nullable = false)
    private String formConfig = "[]";

    /** JSON con el estilo del QR: {preset,dotColor,bgColor,withLogo}. Null = clasico. */
    @Column(name = "qr_style")
    private String qrStyle;
}
