package com.sumaup360.erp.appointment.service;

import com.sumaup360.erp.appointment.repository.AppointmentRepository;

import java.security.SecureRandom;
import java.util.UUID;

/** Genera codigos cortos de ticket (sin caracteres confundibles: 0/O, 1/I/L). */
final class TicketCodes {

    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private TicketCodes() {
    }

    /** Codigo unico dentro del tenant (reintenta ante colision, improbable con 31^6). */
    static String next(AppointmentRepository repository, UUID tenantId) {
        for (int i = 0; i < 10; i++) {
            String code = random();
            if (!repository.existsByTenantIdAndTicketCode(tenantId, code)) {
                return code;
            }
        }
        throw new IllegalStateException("No se pudo generar un codigo de ticket unico.");
    }

    private static String random() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
