package com.sumaup360.app.taxi.dto;

import com.sumaup360.app.taxi.domain.AttachedFile;
import com.sumaup360.app.taxi.domain.QrCustomer;
import com.sumaup360.app.taxi.domain.ReceiptRequest;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** DTOs del modulo Taxista (QR + solicitudes de comprobante). */
public final class TaxiDtos {

    private TaxiDtos() {
    }

    // --- Taxista (app) ---

    public record QrTokenView(String token) {
    }

    public record RequestView(
            UUID id, String tipo, BigDecimal monto, BigDecimal montoEditado,
            String docType, String docNumber, String customerName, String whatsapp, String email,
            String observacion, String estado, String motivo,
            OffsetDateTime createdAt, OffsetDateTime completedAt) {
        public static RequestView from(ReceiptRequest r) {
            return new RequestView(r.getId(), r.getTipo() != null ? r.getTipo().name() : null,
                    r.getMonto(), r.getMontoEditado(), r.getDocType(), r.getDocNumber(),
                    r.getCustomerName(), r.getWhatsapp(), r.getEmail(), r.getObservacion(),
                    r.getEstado() != null ? r.getEstado().name() : null, r.getMotivo(),
                    r.getCreatedAt(), r.getCompletedAt());
        }
    }

    public record EditMontoRequest(@NotNull @Positive BigDecimal monto, @Size(max = 500) String motivo) {
    }

    public record RejectRequest(@Size(max = 500) String motivo) {
    }

    // --- Publico (cliente final del QR) ---

    /** Datos publicos del QR: si es valido y el nombre del taxista (para dar confianza). */
    public record QrPublicView(boolean valid, String taxistaName) {
    }

    /**
     * Precarga por documento. Por privacidad NO devuelve datos de contacto (whatsapp/correo)
     * de la base global de clientes: solo el nombre y el tipo de documento para autocompletar.
     */
    public record CustomerLookupView(boolean found, String docType, String docNumber, String name) {
        public static CustomerLookupView of(QrCustomer c) {
            if (c == null) return new CustomerLookupView(false, null, null, null);
            return new CustomerLookupView(true, c.getDocType(), c.getDocNumber(), c.getName());
        }
    }

    public record CreatePublicRequest(
            @NotBlank String tipo,                    // BOLETA | FACTURA
            @NotNull @Positive @DecimalMax(value = "1000000.00", message = "El monto es demasiado alto.")
            BigDecimal monto,
            @Size(max = 10) String docType,
            @Size(max = 15) @Pattern(regexp = "^[0-9]{0,15}$", message = "El documento debe ser numerico.")
            String docNumber,
            @Size(max = 200) String name,
            @Size(max = 20) @Pattern(regexp = "^[0-9+ ]{6,20}$", message = "WhatsApp invalido.")
            String whatsapp,
            @Size(max = 160) @Email(message = "Correo invalido.")
            String email,
            @Size(max = 500) String observacion) {
    }

    public record CreatedView(UUID requestId, String estado) {
    }

    // --- Backoffice ---

    public record SetStatusRequest(@NotNull String estado, @Size(max = 500) String motivo) {
    }

    public record AttachRequest(@NotNull String fileUrl, @Size(max = 200) String fileName) {
    }

    public record AttachmentView(UUID id, String fileUrl, String fileName, OffsetDateTime createdAt) {
        public static AttachmentView from(AttachedFile a) {
            return new AttachmentView(a.getId(), a.getFileUrl(), a.getFileName(), a.getCreatedAt());
        }
    }
}
