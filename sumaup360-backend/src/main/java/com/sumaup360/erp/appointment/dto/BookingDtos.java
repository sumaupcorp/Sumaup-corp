package com.sumaup360.erp.appointment.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.sumaup360.erp.appointment.domain.BookingPage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BookingDtos {

    private BookingDtos() {
    }

    // ---- Configuracion (panel SaaS) ----

    public record UpdateBookingPageRequest(
            Boolean enabled,
            @Size(max = 120) String title,
            @Size(max = 500) String logoUrl,
            @Size(max = 300) String welcomeText,
            JsonNode formConfig,
            JsonNode qrStyle
    ) {
    }

    public record BookingPageView(UUID id, UUID companyId, String token, boolean enabled,
                                  String title, String logoUrl, String welcomeText,
                                  JsonNode formConfig, JsonNode qrStyle) {
        public static BookingPageView from(BookingPage p, JsonNode formConfig, JsonNode qrStyle) {
            return new BookingPageView(p.getId(), p.getCompanyId(), p.getToken(), p.isEnabled(),
                    p.getTitle(), p.getLogoUrl(), p.getWelcomeText(), formConfig, qrStyle);
        }
    }

    // ---- Publico (web de reserva) ----

    public record BranchOption(UUID id, String name) {
    }

    public record PublicBookingInfo(boolean valid, String title, String logoUrl, String welcomeText,
                                    JsonNode formConfig, List<BranchOption> branches) {
    }

    public record PublicBookingRequest(
            @NotNull UUID branchId,
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 30) String phone,
            @NotNull OffsetDateTime preferredAt,
            @Size(max = 80) String patientName,
            Map<String, String> answers
    ) {
    }

    /** Respuesta al crear la reserva: datos para que la web muestre y descargue el ticket. */
    public record PublicBookingCreated(boolean ok, String message, String ticketCode,
                                       String businessName, String branchName,
                                       String customerName, OffsetDateTime preferredAt) {
    }
}
