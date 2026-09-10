package com.sumaup360.erp.patient.dto;

import com.sumaup360.erp.patient.domain.Patient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class PatientDtos {

    private PatientDtos() {
    }

    public record CreatePatientRequest(
            @NotNull UUID customerId,
            @NotBlank @Size(max = 80) String name,
            @Size(max = 40) String species,
            @Size(max = 60) String breed,
            @Size(max = 10) String sex,
            LocalDate birthDate,
            BigDecimal weightKg,
            @Size(max = 500) String notes,
            @Size(max = 500) String photoUrl
    ) {
    }

    public record UpdatePatientRequest(
            @Size(max = 80) String name,
            @Size(max = 40) String species,
            @Size(max = 60) String breed,
            @Size(max = 10) String sex,
            LocalDate birthDate,
            BigDecimal weightKg,
            @Size(max = 500) String notes,
            @Size(max = 500) String photoUrl,
            Boolean active
    ) {
    }

    public record PatientResponse(UUID id, UUID customerId, String customerName, String name,
                                  String species, String breed, String sex, LocalDate birthDate,
                                  BigDecimal weightKg, String notes, String photoUrl, boolean active) {
        public static PatientResponse from(Patient p, String customerName) {
            return new PatientResponse(p.getId(), p.getCustomerId(), customerName, p.getName(),
                    p.getSpecies(), p.getBreed(), p.getSex(), p.getBirthDate(),
                    p.getWeightKg(), p.getNotes(), p.getPhotoUrl(), p.isActive());
        }
    }
}
