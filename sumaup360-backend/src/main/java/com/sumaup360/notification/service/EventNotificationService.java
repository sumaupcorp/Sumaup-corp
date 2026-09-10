package com.sumaup360.notification.service;

import com.sumaup360.notification.domain.NotificationSetting;
import com.sumaup360.notification.repository.NotificationSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Notificaciones automaticas disparadas por eventos de negocio (p.ej. el staff procesa u
 * observa un comprobante). Lee la configuracion (notification.notification_setting): si el
 * evento esta deshabilitado no envia nada; si esta habilitado usa el title/body del setting.
 *
 * Fail-safe como PushService: nunca rompe la operacion de negocio que dispara el evento.
 */
@Service
public class EventNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EventNotificationService.class);

    private final NotificationSettingRepository settingRepository;
    private final PushService pushService;

    public EventNotificationService(NotificationSettingRepository settingRepository,
                                    PushService pushService) {
        this.settingRepository = settingRepository;
        this.pushService = pushService;
    }

    /**
     * Envia al usuario la notificacion configurada para el evento (key del setting,
     * p.ej. RECEIPT_PROCESSED). Si el setting no existe o esta deshabilitado, no envia.
     */
    public void sendEvent(String settingKey, UUID userId, String route) {
        try {
            NotificationSetting setting = settingRepository.findById(settingKey).orElse(null);
            if (setting == null) {
                log.warn("Evento de notificacion sin configuracion: {} (no se envia push).", settingKey);
                return;
            }
            if (!setting.isEnabled()) {
                log.debug("Evento {} deshabilitado por configuracion; push omitido.", settingKey);
                return;
            }
            Map<String, String> data = route != null && !route.isBlank()
                    ? Map.of("route", route) : Map.of();
            pushService.sendToUser(userId, setting.getTitle(), setting.getBody(), data);
        } catch (Exception e) {
            log.error("Error enviando la notificacion del evento {} al usuario {} "
                    + "(la operacion de negocio no se afecta): {}", settingKey, userId, e.getMessage());
        }
    }
}
