package com.sumaup360.notification.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.sumaup360.notification.domain.DeviceToken;
import com.sumaup360.notification.repository.DeviceTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Envio de notificaciones push (FCM) a los dispositivos de un usuario de la app movil.
 *
 * Fail-safe por diseno: un push nunca debe romper la operacion de negocio que lo dispara.
 * Cualquier excepcion se loguea y NO se propaga. Si Firebase no esta inicializado
 * (arranque local sin credenciales) el envio es un no-op.
 */
@Service
public class PushService {

    private static final Logger log = LoggerFactory.getLogger(PushService.class);

    /** Limite de tokens por llamada multicast de FCM. */
    private static final int FCM_MULTICAST_LIMIT = 500;

    private final DeviceTokenRepository deviceTokenRepository;

    public PushService(DeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
    }

    /**
     * Envia una notificacion a todos los dispositivos registrados del usuario.
     * Si el usuario no tiene tokens, no hace nada (log debug). Los tokens que FCM
     * reporta como invalidos (UNREGISTERED / INVALID_ARGUMENT) se eliminan de la BD.
     */
    public void sendToUser(UUID userId, String title, String body, Map<String, String> data) {
        try {
            List<DeviceToken> devices = deviceTokenRepository.findByUserId(userId);
            if (devices.isEmpty()) {
                log.debug("Push omitido: el usuario {} no tiene dispositivos registrados.", userId);
                return;
            }
            if (FirebaseApp.getApps().isEmpty()) {
                log.debug("Push omitido: Firebase Admin no esta inicializado (sin credenciales).");
                return;
            }

            List<String> tokens = devices.stream()
                    .map(DeviceToken::getToken)
                    .limit(FCM_MULTICAST_LIMIT)
                    .toList();

            MulticastMessage message = MulticastMessage.builder()
                    .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                    .putAllData(data != null ? data : Map.of())
                    .addAllTokens(tokens)
                    .build();

            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);

            int removed = 0;
            List<SendResponse> results = response.getResponses();
            for (int i = 0; i < results.size(); i++) {
                SendResponse r = results.get(i);
                if (r.isSuccessful()) {
                    continue;
                }
                FirebaseMessagingException ex = r.getException();
                MessagingErrorCode code = ex != null ? ex.getMessagingErrorCode() : null;
                if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                    deviceTokenRepository.deleteByToken(tokens.get(i));
                    removed++;
                    log.debug("Token invalido eliminado ({}): {}", code, mask(tokens.get(i)));
                } else {
                    log.warn("Fallo el envio de push al token {}: {}", mask(tokens.get(i)),
                            ex != null ? ex.getMessage() : "error desconocido");
                }
            }
            log.info("Push a usuario {}: {} enviados, {} fallidos, {} tokens eliminados.",
                    userId, response.getSuccessCount(), response.getFailureCount(), removed);
        } catch (Exception e) {
            log.error("Error enviando push al usuario {} (la operacion de negocio no se afecta): {}",
                    userId, e.getMessage());
        }
    }

    /** Muestra solo el inicio del token para no exponerlo completo en logs. */
    private static String mask(String token) {
        if (token == null) return "null";
        return token.length() <= 12 ? "***" : token.substring(0, 12) + "...";
    }
}
