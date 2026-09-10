package com.sumaup360.app.domain;

import com.sumaup360.app.enums.ClientType;
import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Perfil tributario de una persona (un registro por usuario). */
@Entity
@Table(name = "person_profile", schema = "app")
@Getter
@Setter
public class PersonProfile extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "first_name", length = 80)
    private String firstName;

    @Column(name = "last_name", length = 80)
    private String lastName;

    /** El usuario termino el onboarding (wizard). Distinto de profileCompleted (requiere SUNAT). */
    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted = false;

    @Column(name = "segment_code", length = 40)
    private String segmentCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", length = 20)
    private ClientType clientType;

    @Column(name = "dni", length = 15)
    private String dni;

    @Column(name = "ruc", length = 11)
    private String ruc;

    @Column(name = "regime", length = 40)
    private String regime;

    @Column(name = "country_code", length = 5)
    private String countryCode;

    @Column(name = "referral_code", length = 40)
    private String referralCode;

    @Column(name = "profile_completed", nullable = false)
    private boolean profileCompleted = false;

    /** El diagnostico con IA es gratis pero de un solo intento; soporte lo reactiva. */
    @Column(name = "diagnosis_available", nullable = false)
    private boolean diagnosisAvailable = true;

    /** Modo de login SOL elegido: 'ruc' (ruc+usuario+clave) o 'dni' (dni+clave). */
    @Column(name = "sol_doc_mode", length = 3)
    private String solDocMode;

    @Column(name = "sol_user", length = 64)
    private String solUser;

    /** Clave SOL del usuario cifrada (AES-GCM). Solo se descifra en reveal autorizado. */
    @Column(name = "sol_pass_enc", columnDefinition = "text")
    private String solPassEnc;

    // --- Datos fiscales de la Ficha RUC (SUNAT), consultados por sumaup360-sunat ---

    @Column(name = "tax_status", length = 40)
    private String taxStatus;

    @Column(name = "tax_condition", length = 40)
    private String taxCondition;

    @Column(name = "taxpayer_type", length = 80)
    private String taxpayerType;

    @Column(name = "economic_activity", length = 200)
    private String economicActivity;

    @Column(name = "ciiu_code", length = 10)
    private String ciiuCode;

    @Column(name = "ruc_checked_at")
    private java.time.OffsetDateTime rucCheckedAt;

    @Column(name = "razon_social", length = 200)
    private String razonSocial;

    @Column(name = "fecha_inscripcion", length = 20)
    private String fechaInscripcion;

    @Column(name = "fecha_inicio_actividades", length = 20)
    private String fechaInicioActividades;

    @Column(name = "domicilio_fiscal", length = 300)
    private String domicilioFiscal;

    /** Ficha RUC completa (JSON literal de SUNAT) para analisis/IA. */
    @Column(name = "sunat_raw", columnDefinition = "text")
    private String sunatRaw;

    // --- Orientacion tributaria del onboarding (usuarios sin RUC) ---

    /** NULL | 'PENDING' | 'COMPLETED' | 'NOT_REQUIRED'. COMPLETED solo lo pone el analisis. */
    @Column(name = "orientation_status", length = 20)
    private String orientationStatus;

    /** Resultado de la orientacion (JSON: headline, regime, summary, reasons, modelo). */
    @Column(name = "orientation_result", columnDefinition = "text")
    private String orientationResult;
}
