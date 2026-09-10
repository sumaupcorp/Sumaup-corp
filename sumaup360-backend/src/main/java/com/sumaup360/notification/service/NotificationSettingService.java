package com.sumaup360.notification.service;

import com.sumaup360.common.error.BadRequestException;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.notification.domain.NotificationSetting;
import com.sumaup360.notification.repository.NotificationSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Administracion (Backoffice) de la configuracion de notificaciones automaticas:
 * activar/desactivar cada evento o recordatorio y editar sus textos. Las keys se
 * siembran por migracion; no se crean ni eliminan por API.
 */
@Service
public class NotificationSettingService {

    private final NotificationSettingRepository settingRepository;

    public NotificationSettingService(NotificationSettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationSetting> list() {
        return settingRepository.findAllByOrderByKeyAsc();
    }

    @Transactional
    public NotificationSetting update(String key, boolean enabled, String title, String body) {
        if (title == null || title.isBlank()) {
            throw new BadRequestException("El titulo de la notificacion es obligatorio.");
        }
        if (body == null || body.isBlank()) {
            throw new BadRequestException("El mensaje de la notificacion es obligatorio.");
        }
        NotificationSetting setting = settingRepository.findById(key)
                .orElseThrow(() -> new ResourceNotFoundException("Configuracion de notificacion no encontrada."));
        setting.setEnabled(enabled);
        setting.setTitle(title.trim());
        setting.setBody(body.trim());
        return settingRepository.save(setting);
    }
}
