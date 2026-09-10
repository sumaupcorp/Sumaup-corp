package com.sumaup360.notification.service;

import com.sumaup360.notification.domain.DeviceToken;
import com.sumaup360.notification.repository.DeviceTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Registro y baja de tokens FCM de dispositivos de la app movil. */
@Service
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;

    public DeviceTokenService(DeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
    }

    /**
     * Upsert por token: si ya existe se reasigna al usuario autenticado (otro usuario
     * inicio sesion en el mismo dispositivo); si no, se crea.
     */
    @Transactional
    public void register(UUID userId, String token, String platform) {
        DeviceToken dt = deviceTokenRepository.findByToken(token).orElseGet(() -> {
            DeviceToken n = new DeviceToken();
            n.setToken(token);
            return n;
        });
        dt.setUserId(userId);
        dt.setPlatform(platform);
        deviceTokenRepository.save(dt);
    }

    /** Elimina el token si pertenece al usuario autenticado (cierre de sesion). Idempotente. */
    @Transactional
    public void unregister(UUID userId, String token) {
        deviceTokenRepository.deleteByUserIdAndToken(userId, token);
    }
}
