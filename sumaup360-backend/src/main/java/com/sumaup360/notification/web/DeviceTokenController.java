package com.sumaup360.notification.web;

import com.sumaup360.notification.dto.DeviceTokenDtos.AckResponse;
import com.sumaup360.notification.dto.DeviceTokenDtos.RegisterDeviceRequest;
import com.sumaup360.notification.dto.DeviceTokenDtos.UnregisterDeviceRequest;
import com.sumaup360.notification.service.DeviceTokenService;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Registro del token FCM del dispositivo del usuario autenticado (app movil). */
@RestController
@RequestMapping("/api/v1/app/notifications/device")
@Tag(name = "Personas - Push", description = "Registro de dispositivos para notificaciones push")
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    public DeviceTokenController(DeviceTokenService deviceTokenService) {
        this.deviceTokenService = deviceTokenService;
    }

    @PutMapping
    @Operation(summary = "Registra o reasigna el token FCM del dispositivo")
    public AckResponse register(@Valid @RequestBody RegisterDeviceRequest req) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        deviceTokenService.register(userId, req.token(), req.platform());
        return AckResponse.accepted();
    }

    @DeleteMapping
    @Operation(summary = "Da de baja el token FCM del dispositivo (cierre de sesion)")
    public AckResponse unregister(@Valid @RequestBody UnregisterDeviceRequest req) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        deviceTokenService.unregister(userId, req.token());
        return AckResponse.accepted();
    }
}
