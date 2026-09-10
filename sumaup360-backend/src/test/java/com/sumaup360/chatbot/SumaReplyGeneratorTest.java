package com.sumaup360.chatbot;

import com.sumaup360.chatbot.service.SumaReplyGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas del generador de respuestas de Suma (logica pura, sin Spring). */
class SumaReplyGeneratorTest {

    private final SumaReplyGenerator suma = new SumaReplyGenerator();

    @Test
    void greetingWhenEmpty() {
        assertTrue(suma.reply("").toLowerCase().contains("suma"));
    }

    @Test
    void explainsRus() {
        String r = suma.reply("Que es el nuevo RUS?");
        assertTrue(r.contains("RUS"));
        assertTrue(r.contains("contador")); // disclaimer presente
    }

    @Test
    void answersDeclarations() {
        assertTrue(suma.reply("cuando declaro?").toLowerCase().contains("declaracion")
                || suma.reply("cuando vence?").toLowerCase().contains("vence"));
    }

    @Test
    void greetingHasNoDisclaimer() {
        // El saludo simple no necesita disclaimer de contador.
        assertFalse(suma.reply("hola").contains("consulta con un contador"));
    }
}
