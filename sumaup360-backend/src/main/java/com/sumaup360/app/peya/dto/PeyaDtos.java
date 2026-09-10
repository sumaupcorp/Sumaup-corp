package com.sumaup360.app.peya.dto;

import com.sumaup360.app.peya.domain.PeyaUpload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

/** DTOs del modulo Delivery/Peya. */
public final class PeyaDtos {

    private PeyaDtos() {
    }

    // --- App (repartidor) ---

    public record CreateUploadRequest(
            @NotBlank @Size(max = 7) String periodo,   // AAAA-MM
            @NotBlank String pdfUrl,
            @Size(max = 500) String observacion) {
    }

    public record UploadView(
            UUID id, String ruc, String periodo, String pdfUrl, String estado,
            String observacionUsuario, String observacionBackoffice, String codigoNps,
            OffsetDateTime createdAt, OffsetDateTime completedAt) {
        public static UploadView from(PeyaUpload u) {
            return new UploadView(u.getId(), u.getRuc(), u.getPeriodo(), u.getPdfUrl(),
                    u.getEstado() != null ? u.getEstado().name() : null,
                    u.getObservacionUsuario(), u.getObservacionBackoffice(), u.getCodigoNps(),
                    u.getCreatedAt(), u.getCompletedAt());
        }
    }

    // --- Backoffice ---

    public record SetPeyaStatusRequest(@NotBlank String estado, @Size(max = 500) String observacion) {
    }

    public record SetNpsRequest(@NotBlank @Size(max = 40) String codigoNps) {
    }

    public record PeyaAttachRequest(@NotBlank String fileUrl, @Size(max = 200) String fileName) {
    }
}
