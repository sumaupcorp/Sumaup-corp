package com.sumaup360.common.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Parametros anti-abuso de los endpoints publicos (prefijo `public-endpoints`).
 * Los topes diarios son globales por token; cuando exista la membresia por segmento,
 * el valor efectivo debera leerse del plan del dueño del token y estos quedan como techo.
 */
@Component
@ConfigurationProperties(prefix = "public-endpoints")
public class PublicEndpointProperties {

    /** Confiar en X-Forwarded-For SOLO cuando el backend esta detras de un proxy propio. */
    private boolean trustForwardedHeaders = false;

    /** Tope diario de reservas de cita creadas por token de pagina de reserva. */
    private int bookingDailyLimit = 100;

    /** Tope diario de solicitudes de comprobante creadas por token de QR taxista. */
    private int qrDailyLimit = 100;

    public boolean isTrustForwardedHeaders() {
        return trustForwardedHeaders;
    }

    public void setTrustForwardedHeaders(boolean trustForwardedHeaders) {
        this.trustForwardedHeaders = trustForwardedHeaders;
    }

    public int getBookingDailyLimit() {
        return bookingDailyLimit;
    }

    public void setBookingDailyLimit(int bookingDailyLimit) {
        this.bookingDailyLimit = bookingDailyLimit;
    }

    public int getQrDailyLimit() {
        return qrDailyLimit;
    }

    public void setQrDailyLimit(int qrDailyLimit) {
        this.qrDailyLimit = qrDailyLimit;
    }
}
