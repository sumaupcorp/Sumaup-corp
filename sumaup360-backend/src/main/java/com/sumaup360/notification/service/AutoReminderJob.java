package com.sumaup360.notification.service;

import com.sumaup360.app.enums.ReceiptStatus;
import com.sumaup360.app.repository.PersonProfileRepository;
import com.sumaup360.app.repository.ReceiptRepository;
import com.sumaup360.notification.domain.NotificationSetting;
import com.sumaup360.notification.repository.DeviceTokenRepository;
import com.sumaup360.notification.repository.NotificationSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Recordatorios automaticos diarios (10:00 America/Lima) a usuarios de la app movil.
 * Solo se envian los recordatorios habilitados en notification.notification_setting
 * (REMINDER_*), con el title/body configurado por el staff en el Backoffice.
 */
@Component
public class AutoReminderJob {

    private static final Logger log = LoggerFactory.getLogger(AutoReminderJob.class);

    /** Ruta que abre la app al tocar el recordatorio. */
    private static final String REMINDER_ROUTE = "/home";

    public static final String REMINDER_ONBOARDING = "REMINDER_ONBOARDING";
    public static final String REMINDER_ORIENTATION = "REMINDER_ORIENTATION";
    public static final String REMINDER_RECEIPTS_OBSERVED = "REMINDER_RECEIPTS_OBSERVED";

    private final NotificationSettingRepository settingRepository;
    private final PersonProfileRepository profileRepository;
    private final ReceiptRepository receiptRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final PushService pushService;

    public AutoReminderJob(NotificationSettingRepository settingRepository,
                           PersonProfileRepository profileRepository,
                           ReceiptRepository receiptRepository,
                           DeviceTokenRepository deviceTokenRepository,
                           PushService pushService) {
        this.settingRepository = settingRepository;
        this.profileRepository = profileRepository;
        this.receiptRepository = receiptRepository;
        this.deviceTokenRepository = deviceTokenRepository;
        this.pushService = pushService;
    }

    @Scheduled(cron = "0 0 10 * * *", zone = "America/Lima")
    public void sendDailyReminders() {
        sendReminder(REMINDER_ONBOARDING,
                profileRepository::findUserIdsWithOnboardingIncomplete);
        sendReminder(REMINDER_ORIENTATION,
                () -> profileRepository.findUserIdsByOrientationStatus("PENDING"));
        sendReminder(REMINDER_RECEIPTS_OBSERVED,
                () -> receiptRepository.findDistinctUserIdsByStatus(ReceiptStatus.OBSERVED));
    }

    private void sendReminder(String settingKey, AudienceSupplier audienceSupplier) {
        try {
            NotificationSetting setting = settingRepository.findById(settingKey).orElse(null);
            if (setting == null || !setting.isEnabled()) {
                log.debug("Recordatorio {} deshabilitado o sin configuracion; omitido.", settingKey);
                return;
            }
            List<UUID> userIds = audienceSupplier.get();
            Map<String, String> data = Map.of("route", REMINDER_ROUTE);

            int sent = 0;
            int withoutDevices = 0;
            for (UUID userId : userIds) {
                if (deviceTokenRepository.existsByUserId(userId)) {
                    pushService.sendToUser(userId, setting.getTitle(), setting.getBody(), data);
                    sent++;
                } else {
                    withoutDevices++;
                }
            }
            log.info("Recordatorio {}: audiencia {}, {} enviados, {} sin dispositivos.",
                    settingKey, userIds.size(), sent, withoutDevices);
        } catch (Exception e) {
            log.error("Error enviando el recordatorio {}: {}", settingKey, e.getMessage());
        }
    }

    @FunctionalInterface
    private interface AudienceSupplier {
        List<UUID> get();
    }
}
