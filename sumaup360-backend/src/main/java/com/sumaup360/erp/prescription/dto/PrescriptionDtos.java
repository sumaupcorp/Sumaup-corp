package com.sumaup360.erp.prescription.dto;

import com.sumaup360.erp.prescription.domain.Prescription;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public final class PrescriptionDtos {

    private PrescriptionDtos() {
    }

    public record CreatePrescriptionRequest(
            @NotNull UUID branchId,
            UUID customerId,
            UUID saleId,
            @NotBlank @Size(max = 120) String doctorName,
            @Size(max = 20) String doctorLicense,
            @NotNull LocalDate issuedDate,
            @Size(max = 200) String diagnosis,
            @NotBlank String medications,
            @Size(max = 300) String notes
    ) {
    }

    public record PrescriptionResponse(UUID id, UUID branchId, UUID customerId, String customerName,
                                       UUID saleId, String doctorName, String doctorLicense,
                                       LocalDate issuedDate, String diagnosis, String medications,
                                       String notes) {
        public static PrescriptionResponse from(Prescription p, String customerName) {
            return new PrescriptionResponse(p.getId(), p.getBranchId(), p.getCustomerId(), customerName,
                    p.getSaleId(), p.getDoctorName(), p.getDoctorLicense(), p.getIssuedDate(),
                    p.getDiagnosis(), p.getMedications(), p.getNotes());
        }
    }
}
