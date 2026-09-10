package com.sumaup360.backoffice.dto;

import jakarta.validation.constraints.Size;

public final class FiscalDtos {

    private FiscalDtos() {
    }

    public record UpsertCredentialsRequest(
            @Size(max = 11) String ruc,
            @Size(max = 64) String solUser,
            String solPass
    ) {
    }

    /** Vista enmascarada: nunca expone la clave en claro. */
    public record CredentialsView(String ruc, String solUser, boolean hasPassword) {
    }

    /** Resultado del reveal autorizado (clave en claro, transitoria). */
    public record RevealResponse(String solUser, String solPass) {
    }
}
