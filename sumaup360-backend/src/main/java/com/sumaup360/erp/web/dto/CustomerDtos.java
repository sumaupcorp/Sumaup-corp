package com.sumaup360.erp.web.dto;

import com.sumaup360.erp.domain.Customer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class CustomerDtos {

    private CustomerDtos() {
    }

    public record CreateCustomerRequest(
            @NotNull UUID companyId,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 20) String docType,
            @Size(max = 20) String docNumber,
            @Size(max = 255) String email,
            @Size(max = 30) String phone
    ) {
    }

    public record CustomerResponse(UUID id, String name, String docType, String docNumber,
                                   String email, String phone) {
        public static CustomerResponse from(Customer c) {
            return new CustomerResponse(c.getId(), c.getName(), c.getDocType(),
                    c.getDocNumber(), c.getEmail(), c.getPhone());
        }
    }

    /** Cita resumida para el historial de la ficha del cliente. */
    public record AppointmentBrief(UUID id, OffsetDateTime scheduledAt, String status,
                                   String reason, String patientName) {
    }

    /** Compra resumida para el historial de la ficha del cliente. */
    public record SaleBrief(UUID id, OffsetDateTime createdAt, BigDecimal total,
                            String paymentMethod, int itemCount) {
    }

    /**
     * Ficha del cliente: datos + estadisticas de atencion (citas, visitas, compras)
     * + historial reciente. Todo en una sola llamada.
     */
    public record CustomerSummaryResponse(
            CustomerResponse customer,
            long totalAppointments,
            long attended,
            long canceled,
            long noShow,
            long upcoming,
            long totalPurchases,
            BigDecimal totalSpent,
            OffsetDateTime lastVisitAt,
            List<String> patients,
            List<AppointmentBrief> recentAppointments,
            List<SaleBrief> recentSales
    ) {
    }
}
