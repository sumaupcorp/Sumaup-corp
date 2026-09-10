package com.sumaup360.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public final class DeviceTokenDtos {

    private DeviceTokenDtos() {
    }

    public record RegisterDeviceRequest(
            @NotBlank String token,
            @NotBlank @Pattern(regexp = "android|ios|web",
                    message = "La plataforma debe ser android, ios o web.") String platform
    ) {
    }

    public record UnregisterDeviceRequest(@NotBlank String token) {
    }

    public record AckResponse(boolean ok) {
        public static AckResponse accepted() {
            return new AckResponse(true);
        }
    }
}
