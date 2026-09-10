package com.sumaup360.app.honorarios.dto;

import com.sumaup360.app.honorarios.domain.HonorarioRequest;
import com.sumaup360.app.honorarios.domain.SuspensionRequest;
import com.sumaup360.app.taxi.domain.AttachedFile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** DTOs del modulo Servicios Profesionales (Recibos por Honorarios + Suspension de 4ta). */
public final class HonorariosDtos {

    private HonorariosDtos() {
    }

    // --- Recibos por Honorarios ---

    public record CreateHonorarioRequest(
            @NotBlank @Size(max = 200) String clienteNombre,
            @Size(max = 10) String clienteDocType,
            @Size(max = 15) String clienteDocNumber,
            @NotBlank @Size(max = 500) String descripcion,
            @NotNull @Positive BigDecimal monto,
            Boolean conRetencion) {
    }

    public record HonorarioView(
            UUID id, String clienteNombre, String clienteDocType, String clienteDocNumber,
            String descripcion, BigDecimal monto, boolean conRetencion, String estado,
            String reciboUrl, String observacion, OffsetDateTime createdAt, OffsetDateTime completedAt) {
        public static HonorarioView from(HonorarioRequest h) {
            return new HonorarioView(h.getId(), h.getClienteNombre(), h.getClienteDocType(),
                    h.getClienteDocNumber(), h.getDescripcion(), h.getMonto(), h.isConRetencion(),
                    h.getEstado() != null ? h.getEstado().name() : null, h.getReciboUrl(),
                    h.getObservacion(), h.getCreatedAt(), h.getCompletedAt());
        }
    }

    // --- Suspension de 4ta (anual) ---

    public record CreateSuspensionRequest(@NotNull Integer anio) {
    }

    public record SuspensionView(
            UUID id, int anio, String estado, String observacion, String constanciaUrl,
            OffsetDateTime createdAt, OffsetDateTime completedAt) {
        public static SuspensionView from(SuspensionRequest s) {
            return new SuspensionView(s.getId(), s.getAnio(),
                    s.getEstado() != null ? s.getEstado().name() : null, s.getObservacion(),
                    s.getConstanciaUrl(), s.getCreatedAt(), s.getCompletedAt());
        }
    }

    // --- Backoffice (comun) ---

    public record SetStatusRequest(@NotNull String estado, @Size(max = 500) String observacion) {
    }

    public record AttachRequest(@NotBlank String fileUrl, @Size(max = 200) String fileName) {
    }

    public record AttachmentView(UUID id, String fileUrl, String fileName, OffsetDateTime createdAt) {
        public static AttachmentView from(AttachedFile a) {
            return new AttachmentView(a.getId(), a.getFileUrl(), a.getFileName(), a.getCreatedAt());
        }
    }
}
