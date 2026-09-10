package com.sumaup360.app.dto;

import jakarta.validation.constraints.Size;

/** Credenciales SUNAT (Clave SOL) de la persona, cara app movil. */
public final class FiscalSolDtos {

    private FiscalSolDtos() {
    }

    /** Alta/edicion. La clave viaja en claro por TLS y se cifra en reposo (AES-GCM). */
    public record UpsertSolRequest(
            String docMode,                  // "dni" | "ruc" (default ruc)
            @Size(max = 11) String ruc,
            @Size(max = 64) String solUser,
            @Size(max = 15) String dni,
            String solPass
    ) {
    }

    /** Vista enmascarada: nunca expone la Clave SOL en claro. */
    public record SolCredentialsView(String docMode, String ruc, String solUser, String dni, boolean hasPassword) {
    }

    /** Pide validar la Clave SOL con un login real en SUNAT (no guarda nada). */
    public record ValidateSolRequest(
            String docMode,                  // "dni" | "ruc" (default ruc)
            @Size(max = 11) String ruc,
            @Size(max = 64) String solUser,
            String solPass,
            @Size(max = 15) String dni
    ) {
    }

    /** Resultado de la validacion: ok + nombre del titular, o el detalle del error. */
    public record SolValidationView(boolean ok, String nombre, String detail) {
    }

    /** Pide refrescar los datos de la Ficha RUC. Si ruc es null, usa el del perfil. */
    public record RucRefreshRequest(@Size(max = 11) String ruc) {
    }

    /** Consulta por documento (DNI por defecto). docType: 1=DNI,4=CE,7=Pasaporte,A=Ced.Dipl. */
    public record DniRefreshRequest(@Size(max = 15) String dni, String docType) {
    }

    /** Datos fiscales de la Ficha RUC guardados en el perfil. */
    public record RucFiscalView(
            String ruc,
            String razonSocial,
            String taxStatus,
            String taxCondition,
            String taxpayerType,
            String economicActivity,
            String ciiuCode,
            String fechaInscripcion,
            String fechaInicioActividades,
            String domicilioFiscal,
            String checkedAt,
            boolean esValidoParaPerfil
    ) {
    }
}
