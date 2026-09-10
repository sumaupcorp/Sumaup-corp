package com.sumaup360.backoffice.dto;

import com.sumaup360.app.enums.ReceiptStatus;
import com.sumaup360.app.enums.ReceiptType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class BackofficeDtos {

    private BackofficeDtos() {
    }

    /**
     * Vista enriquecida del recibo para el Backoffice: incluye datos del usuario (persona),
     * su RUC/regimen, si tiene Clave SOL guardada, y el estado de declaracion en SIRE/SUNAT.
     */
    public record ReceiptView(
            UUID id,
            UUID userId,
            String userName,
            String userEmail,
            String userRuc,
            String userRegime,
            boolean userHasSol,
            ReceiptType type,
            String docNumber,
            LocalDate issueDate,
            BigDecimal amount,
            String currency,
            String fileUrl,
            String filePath,
            boolean hasFile,
            String issuerRuc,
            ReceiptStatus status,
            UUID assignedStaffId,
            String notes,
            OffsetDateTime processedAt,
            boolean declaredSire,
            String sirePeriod,
            OffsetDateTime sireDeclaredAt,
            boolean declaredSunat,
            String sunatPeriod,
            OffsetDateTime sunatDeclaredAt
    ) {
    }

    public record ProcessReceiptRequest(String note) {
    }

    /** Periodo tributario (formato YYYY-MM) para declarar en SIRE/SUNAT. */
    public record DeclareRequest(String period) {
    }

    /** Datos fiscales del usuario (persona): RUC, usuario y Clave SOL. */
    public record UserFiscalRequest(String ruc, String regime, String solUser, String solPass) {
    }

    public record RevealSolResponse(String solUser, String solPass) {
    }

    /** Ruta del archivo en Storage (la setea la app movil al subir el comprobante). */
    public record AttachFileRequest(String filePath) {
    }

    public record FileUrlResponse(String url, int expiresInSeconds) {
    }
}
