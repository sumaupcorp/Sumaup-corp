package com.sumaup360.app.dto;

/** Perfil de usuario (cara persona) para la app movil. */
public final class ProfileMeDtos {

    private ProfileMeDtos() {
    }

    public record ProfileMeResponse(
            String fullName,
            String email,
            String phone,
            String photoUrl,
            String countryCode,
            String referralCode,
            boolean profileCompleted,
            String segmentCode,
            String ruc,
            String regime,
            String clientType,
            String dni,
            String firstName,
            String lastName,
            boolean onboardingCompleted,
            String orientationStatus
    ) {
    }

    public record UpdateProfileMeRequest(
            String fullName,
            String phone,
            String countryCode,
            String referralCode,
            String photoUrl,
            String clientType,
            String dni,
            String firstName,
            String lastName,
            Boolean onboardingCompleted,
            String orientationStatus
    ) {
    }

    /** Estado de completitud del perfil para el gating de la app. */
    public record ProfileCompletionResponse(
            boolean profileCompleted,
            boolean rucValidoParaPerfil,
            java.util.List<String> missing
    ) {
    }
}
