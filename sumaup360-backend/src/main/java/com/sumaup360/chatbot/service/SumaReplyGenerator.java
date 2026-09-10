package com.sumaup360.chatbot.service;

import org.springframework.stereotype.Component;

/**
 * Generador de respuestas de Suma (asistente). Reglas simples y deterministas.
 * TODO: reemplazar por integracion real con un LLM (Claude) usando el contexto del usuario.
 * Suma orienta; no reemplaza la asesoria de un contador (disclaimer).
 */
@Component
public class SumaReplyGenerator {

    private static final String DISCLAIMER =
            " (Suma te orienta; para decisiones importantes consulta con un contador.)";

    public String reply(String userText) {
        String t = userText == null ? "" : userText.toLowerCase();

        if (t.isBlank()) {
            return "Hola, soy Suma. Cuentame en que te ayudo con tus ingresos, gastos o impuestos.";
        }
        if (t.contains("hola") || t.contains("buenas") || t.contains("buenos dias")) {
            return "Hola, soy Suma. Puedo ayudarte con RUS, declaraciones, boletas y tu orden financiero.";
        }
        if (t.contains("rus")) {
            return "El Nuevo RUS es un regimen para pequenos negocios; pagas una cuota fija mensual "
                    + "segun tus ingresos y no llevas contabilidad compleja." + DISCLAIMER;
        }
        if (t.contains("declar") || t.contains("vence") || t.contains("vencimiento")) {
            return "Tus declaraciones mensuales vencen segun el ultimo digito de tu RUC. "
                    + "Revisa la seccion de Alertas para tus proximas fechas." + DISCLAIMER;
        }
        if (t.contains("boleta") || t.contains("factura") || t.contains("comprobante")) {
            return "Puedes subir tus boletas y facturas desde la app; nuestro equipo las procesa "
                    + "y las asocia a tus ingresos o gastos." + DISCLAIMER;
        }
        if (t.contains("gasto") || t.contains("ingreso")) {
            return "Registra tus ingresos y gastos en la app para conocer tu utilidad real del mes." + DISCLAIMER;
        }
        return "Gracias por tu mensaje. Puedo orientarte sobre RUS, declaraciones, boletas, "
                + "ingresos y gastos. Cuentame un poco mas." + DISCLAIMER;
    }
}
