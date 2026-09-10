package com.sumaup360.erp.documenttemplate.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Monto en letras al formato peruano de comprobantes:
 * 123.50 -> "SON: CIENTO VEINTITRES CON 50/100 SOLES".
 */
public final class SpanishMoneyWords {

    private static final String[] UNIDADES = {
            "", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE",
            "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISEIS", "DIECISIETE",
            "DIECIOCHO", "DIECINUEVE", "VEINTE"
    };
    private static final String[] DECENAS = {
            "", "", "VEINTI", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA",
            "OCHENTA", "NOVENTA"
    };
    private static final String[] CENTENAS = {
            "", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS",
            "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"
    };

    private SpanishMoneyWords() {
    }

    public static String amountInWords(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        BigDecimal value = amount.setScale(2, RoundingMode.HALF_UP);
        long entero = value.longValue();
        int centimos = value.remainder(BigDecimal.ONE).movePointRight(2).abs().intValue();
        String letras = entero == 0 ? "CERO" : toWords(entero);
        return "SON: %s CON %02d/100 SOLES".formatted(letras, centimos);
    }

    private static String toWords(long n) {
        if (n >= 1_000_000) {
            long millones = n / 1_000_000;
            long resto = n % 1_000_000;
            String prefijo = millones == 1 ? "UN MILLON" : toWords(millones) + " MILLONES";
            return resto == 0 ? prefijo : prefijo + " " + toWords(resto);
        }
        if (n >= 1_000) {
            long miles = n / 1_000;
            long resto = n % 1_000;
            String prefijo = miles == 1 ? "MIL" : toWords(miles) + " MIL";
            return resto == 0 ? prefijo : prefijo + " " + toWords(resto);
        }
        if (n == 100) {
            return "CIEN";
        }
        if (n > 100) {
            long resto = n % 100;
            String prefijo = CENTENAS[(int) (n / 100)];
            return resto == 0 ? prefijo : prefijo + " " + toWords(resto);
        }
        if (n <= 20) {
            return UNIDADES[(int) n];
        }
        int decena = (int) (n / 10);
        int unidad = (int) (n % 10);
        if (decena == 2) {
            return unidad == 0 ? "VEINTE" : DECENAS[2] + UNIDADES[unidad];
        }
        return unidad == 0 ? DECENAS[decena] : DECENAS[decena] + " Y " + UNIDADES[unidad];
    }
}
