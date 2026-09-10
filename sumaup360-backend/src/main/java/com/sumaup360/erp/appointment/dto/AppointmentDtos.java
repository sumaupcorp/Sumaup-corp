package com.sumaup360.erp.appointment.dto;

import com.sumaup360.erp.appointment.domain.Appointment;
import com.sumaup360.erp.appointment.enums.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class AppointmentDtos {

    private AppointmentDtos() {
    }

    public record CreateAppointmentRequest(
            @NotNull UUID branchId,
            @NotNull UUID customerId,
            UUID patientId,
            @Size(max = 160) String reason,
            @NotNull OffsetDateTime scheduledAt,
            @Positive Integer durationMinutes,
            UUID assignedTo,
            @Size(max = 500) String notes
    ) {
    }

    public record UpdateAppointmentRequest(
            OffsetDateTime scheduledAt,
            @Positive Integer durationMinutes,
            @Size(max = 160) String reason,
            UUID patientId,
            UUID assignedTo,
            @Size(max = 500) String notes
    ) {
    }

    public record UpdateAppointmentStatusRequest(@NotNull AppointmentStatus status) {
    }

    public record AppointmentResponse(UUID id, UUID branchId, UUID customerId, String customerName,
                                      UUID patientId, String patientName, String reason,
                                      OffsetDateTime scheduledAt, int durationMinutes,
                                      AppointmentStatus status, UUID assignedTo, String notes,
                                      String source, String formData, String ticketCode) {
        public static AppointmentResponse from(Appointment a, String customerName, String patientName) {
            return new AppointmentResponse(a.getId(), a.getBranchId(), a.getCustomerId(), customerName,
                    a.getPatientId(), patientName, a.getReason(), a.getScheduledAt(),
                    a.getDurationMinutes(), a.getStatus(), a.getAssignedTo(), a.getNotes(),
                    a.getSource(), a.getFormData(), a.getTicketCode());
        }
    }
}
