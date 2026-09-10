package com.sumaup360.app.dto;

import com.sumaup360.app.domain.Alert;
import com.sumaup360.app.enums.AlertStatus;
import com.sumaup360.app.enums.AlertType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public final class AlertDtos {

    private AlertDtos() {
    }

    public record CreateAlertRequest(
            AlertType type,
            @NotBlank String title,
            String message,
            LocalDate dueDate
    ) {
    }

    public record UpdateAlertStatusRequest(@NotNull AlertStatus status) {
    }

    public record AlertResponse(UUID id, AlertType type, String title, String message,
                                LocalDate dueDate, AlertStatus status) {
        public static AlertResponse from(Alert a) {
            return new AlertResponse(a.getId(), a.getType(), a.getTitle(), a.getMessage(),
                    a.getDueDate(), a.getStatus());
        }
    }
}
