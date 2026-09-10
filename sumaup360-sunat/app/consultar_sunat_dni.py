#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Consultar SUNAT por DNI/RUC/Carnet/Pasaporte usando Playwright.

Instalación:
    pip install playwright requests python-dotenv
    python -m playwright install chromium

Uso:
    python consultar_sunat_dni.py 73420557
    python consultar_sunat_dni.py 73420557 --tipo 1 --headless false
    python consultar_sunat_dni.py 10734205571 --modo ruc

Variables opcionales en .env:
    BACKEND_URL=http://localhost:8080/api/sunat/contribuyentes
    BACKEND_TOKEN=tu_token_jwt
"""

import argparse
import asyncio
import json
import os
import re
import sys
from datetime import datetime
from typing import Any, Dict, List, Optional

import requests
from dotenv import load_dotenv
from playwright.async_api import async_playwright, TimeoutError as PlaywrightTimeoutError


SUNAT_URL = "https://e-consultaruc.sunat.gob.pe/cl-ti-itmrconsruc/FrameCriterioBusquedaWeb.jsp"


TIPOS_DOCUMENTO = {
    "1": "Documento Nacional de Identidad",
    "4": "Carnet de Extranjeria",
    "7": "Pasaporte",
    "A": "Ced. Diplomática de Identidad",
}


def limpiar_texto(valor: Optional[str]) -> str:
    if not valor:
        return ""
    return re.sub(r"\s+", " ", valor).strip()


def extraer_ruc_desde_texto(texto: str) -> Optional[str]:
    match = re.search(r"\b(\d{11})\b", texto or "")
    return match.group(1) if match else None


def es_sin_ruc(body: str) -> bool:
    return "NO REGISTRA" in (body or "").upper()


def mensaje_sin_ruc(body: str, numero: str) -> str:
    """Devuelve el mensaje literal de SUNAT (si esta) o uno amigable."""
    m = re.search(r"(El Sistema RUC NO REGISTRA[^.]*\.)", body or "", re.IGNORECASE)
    if m:
        return limpiar_texto(m.group(1))
    return f"El documento {numero} no tiene un RUC registrado en SUNAT."


def normalizar_clave(label: str) -> str:
    label = limpiar_texto(label).replace(":", "").lower()
    reemplazos = {
        "número de ruc": "numero_ruc",
        "tipo contribuyente": "tipo_contribuyente",
        "nombre comercial": "nombre_comercial",
        "fecha de inscripción": "fecha_inscripcion",
        "fecha de inicio de actividades": "fecha_inicio_actividades",
        "estado del contribuyente": "estado_contribuyente",
        "condición del contribuyente": "condicion_contribuyente",
        "domicilio fiscal": "domicilio_fiscal",
        "sistema emisión de comprobante": "sistema_emision_comprobante",
        "sistema de contabilidad": "sistema_contabilidad",
        "actividad(es) económica(s)": "actividades_economicas",
        "comprobantes de pago c/aut. de impresión (f. 806 u 816)": "comprobantes_pago",
        "sistema de emisión electrónica": "sistema_emision_electronica",
        "emisor electrónico desde": "emisor_electronico_desde",
        "comprobantes electrónicos": "comprobantes_electronicos",
        "afiliado al ple desde": "afiliado_ple_desde",
        "padrones": "padrones",
    }
    return reemplazos.get(label, re.sub(r"[^a-z0-9]+", "_", label).strip("_"))


async def esperar_resultado_o_detalle(page, timeout: int = 15000) -> str:
    try:
        await page.wait_for_load_state("networkidle", timeout=timeout)
    except Exception:
        pass

    try:
        await page.wait_for_function(
            """
            () => {
                const hayLista = document.querySelectorAll('a.aRucs').length > 0;
                const texto = document.body ? document.body.innerText : '';
                const hayDetalle = texto.includes('Número de RUC:') || texto.includes('Tipo Contribuyente:');
                const txtUpper = texto.toUpperCase();
                const sinResultado = texto.includes('No se encontró') ||
                                     texto.includes('no se encontró') ||
                                     texto.includes('No existe') ||
                                     txtUpper.includes('NO REGISTRA') ||
                                     texto.includes('Ingrese') && texto.includes('válido');
                return hayLista || hayDetalle || sinResultado;
            }
            """,
            timeout=timeout,
        )
    except PlaywrightTimeoutError:
        return "sin_resultado"

    cantidad_rucs = await page.locator("a.aRucs").count()
    if cantidad_rucs > 0:
        return "lista_rucs"

    body = limpiar_texto(await page.locator("body").inner_text())
    if "Número de RUC:" in body or "Tipo Contribuyente:" in body:
        return "detalle"

    return "sin_resultado"


async def extraer_lista_rucs(page) -> List[Dict[str, Any]]:
    items = await page.locator("a.aRucs").evaluate_all(
        r"""
        els => els.map(el => {
            const h4 = [...el.querySelectorAll('h4')].map(x => x.innerText.trim()).filter(Boolean);
            const ps = [...el.querySelectorAll('p')].map(x => x.innerText.trim()).filter(Boolean);
            const text = el.innerText.trim();
            const ruc = el.getAttribute('data-ruc') || (text.match(/\b\d{11}\b/) || [null])[0];
            return {
                ruc,
                titulo_ruc: h4[0] || '',
                razon_social: h4[1] || '',
                textos: ps,
                texto_completo: text
            };
        })
        """
    )

    resultados = []
    for item in items:
        textos = item.get("textos") or []
        ubicacion = ""
        estado = ""

        for t in textos:
            if "Ubicación:" in t:
                ubicacion = limpiar_texto(t.replace("Ubicación:", ""))
            if "Estado:" in t:
                estado = limpiar_texto(t.replace("Estado:", ""))

        resultados.append({
            "ruc": limpiar_texto(item.get("ruc")),
            "razon_social": limpiar_texto(item.get("razon_social")),
            "ubicacion": ubicacion,
            "estado": estado,
            "texto_completo": limpiar_texto(item.get("texto_completo")),
        })

    return resultados


async def extraer_detalle_contribuyente(page) -> Dict[str, Any]:
    data = await page.locator("body").evaluate(
        r"""
        () => {
            const out = {};
            const rows = [...document.querySelectorAll('.list-group-item .row')];

            for (const row of rows) {
                const labelEl = row.querySelector('.col-sm-5 h4, .col-sm-5');
                const valueEl = row.querySelector('.col-sm-7');
                if (!labelEl || !valueEl) continue;

                const label = labelEl.innerText.replace(/\s+/g, ' ').trim().replace(':', '');
                if (!label) continue;

                const tableRows = [...valueEl.querySelectorAll('table tr')]
                    .map(tr => tr.innerText.replace(/\s+/g, ' ').trim())
                    .filter(Boolean);

                if (tableRows.length > 0) {
                    out[label] = tableRows;
                } else {
                    out[label] = valueEl.innerText.replace(/\s+/g, ' ').trim();
                }
            }

            return out;
        }
        """
    )

    normalizado: Dict[str, Any] = {}
    for label, value in data.items():
        key = normalizar_clave(label)
        if isinstance(value, list):
            normalizado[key] = [limpiar_texto(v) for v in value if limpiar_texto(v)]
        else:
            normalizado[key] = limpiar_texto(value)

    numero_ruc_texto = normalizado.get("numero_ruc", "")
    ruc = extraer_ruc_desde_texto(numero_ruc_texto)
    razon_social = ""
    if ruc and " - " in numero_ruc_texto:
        razon_social = limpiar_texto(numero_ruc_texto.split(" - ", 1)[1])

    normalizado["ruc"] = ruc or ""
    normalizado["razon_social"] = razon_social
    normalizado["consultado_en"] = datetime.now().isoformat(timespec="seconds")

    return normalizado


async def consultar_sunat(
    numero: str,
    tipo_doc: str = "1",
    modo: str = "documento",
    headless: bool = True,
    slow_mo: int = 0,
) -> Dict[str, Any]:
    if modo == "documento" and tipo_doc not in TIPOS_DOCUMENTO:
        raise ValueError(f"Tipo de documento inválido: {tipo_doc}. Opciones: {TIPOS_DOCUMENTO}")

    async with async_playwright() as p:
        browser = await p.chromium.launch(
            headless=headless,
            slow_mo=slow_mo,
            args=[
                "--disable-blink-features=AutomationControlled",
                "--disable-dev-shm-usage",
                "--no-sandbox",
            ],
        )

        context = await browser.new_context(
            viewport={"width": 1366, "height": 900},
            locale="es-PE",
            user_agent=(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                "AppleWebKit/537.36 (KHTML, like Gecko) "
                "Chrome/120.0.0.0 Safari/537.36"
            ),
        )

        page = await context.new_page()

        try:
            await page.goto(SUNAT_URL, wait_until="domcontentloaded", timeout=45000)

            if modo == "documento":
                await page.locator("#btnPorDocumento").click(timeout=15000)
                await page.locator("#cmbTipoDoc").select_option(tipo_doc)
                await page.locator("#txtNumeroDocumento").fill(numero)
                await page.locator("#btnAceptar").click()

                estado = await esperar_resultado_o_detalle(page)

                if estado == "lista_rucs":
                    resultados = await extraer_lista_rucs(page)
                    if not resultados:
                        return {
                            "ok": False,
                            "mensaje": "No se encontraron RUC asociados al documento.",
                            "numero_consultado": numero,
                            "tipo_documento": tipo_doc,
                        }

                    index_click = 0
                    for i, item in enumerate(resultados):
                        if "ACTIVO" in (item.get("estado") or "").upper():
                            index_click = i
                            break

                    await page.locator("a.aRucs").nth(index_click).click()
                    await esperar_resultado_o_detalle(page)
                    detalle = await extraer_detalle_contribuyente(page)

                    return {
                        "ok": True,
                        "modo": "documento",
                        "numero_consultado": numero,
                        "tipo_documento": tipo_doc,
                        "tipo_documento_nombre": TIPOS_DOCUMENTO.get(tipo_doc),
                        "rucs_encontrados": resultados,
                        "ruc_seleccionado": resultados[index_click],
                        "detalle": detalle,
                    }

                if estado == "detalle":
                    detalle = await extraer_detalle_contribuyente(page)
                    return {
                        "ok": True,
                        "modo": "documento",
                        "numero_consultado": numero,
                        "tipo_documento": tipo_doc,
                        "tipo_documento_nombre": TIPOS_DOCUMENTO.get(tipo_doc),
                        "rucs_encontrados": [],
                        "detalle": detalle,
                    }

                body_text = limpiar_texto(await page.locator("body").inner_text())
                sin_ruc = es_sin_ruc(body_text)
                return {
                    "ok": False,
                    "sin_ruc": sin_ruc,
                    "mensaje": mensaje_sin_ruc(body_text, numero) if sin_ruc
                    else "SUNAT no devolvió resultados o la página cambió.",
                    "numero_consultado": numero,
                    "tipo_documento": tipo_doc,
                    "html_preview": body_text[:1000],
                }

            if modo == "ruc":
                await page.locator("#btnPorRuc").click(timeout=15000)
                await page.locator("#txtRuc").fill(numero)
                await page.locator("#btnAceptar").click()

                estado = await esperar_resultado_o_detalle(page)
                if estado == "detalle":
                    detalle = await extraer_detalle_contribuyente(page)
                    return {
                        "ok": True,
                        "modo": "ruc",
                        "numero_consultado": numero,
                        "detalle": detalle,
                    }

                body_text = limpiar_texto(await page.locator("body").inner_text())
                sin_ruc = es_sin_ruc(body_text)
                return {
                    "ok": False,
                    "sin_ruc": sin_ruc,
                    "mensaje": mensaje_sin_ruc(body_text, numero) if sin_ruc
                    else "No se encontró detalle por RUC o la página cambió.",
                    "numero_consultado": numero,
                    "html_preview": body_text[:1000],
                }

            raise ValueError("Modo inválido. Usa: documento o ruc")

        finally:
            await context.close()
            await browser.close()


def guardar_json(resultado: Dict[str, Any], salida: str) -> str:
    if salida.lower().endswith(".json"):
        filename = salida
    else:
        os.makedirs(salida, exist_ok=True)
        numero = resultado.get("numero_consultado", "consulta")
        ruc = ""
        if isinstance(resultado.get("detalle"), dict):
            ruc = resultado["detalle"].get("ruc") or ""
        filename = os.path.join(salida, f"sunat_{numero}_{ruc or 'sin_ruc'}.json")

    with open(filename, "w", encoding="utf-8") as f:
        json.dump(resultado, f, ensure_ascii=False, indent=2)

    return filename


def enviar_a_backend(resultado: Dict[str, Any], backend_url: str, token: Optional[str] = None) -> Dict[str, Any]:
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"

    response = requests.post(backend_url, json=resultado, headers=headers, timeout=30)

    try:
        body = response.json()
    except Exception:
        body = response.text

    return {
        "status_code": response.status_code,
        "ok": response.ok,
        "body": body,
    }


def crear_payload_simple(resultado: Dict[str, Any]) -> Dict[str, Any]:
    detalle = resultado.get("detalle") or {}
    seleccionado = resultado.get("ruc_seleccionado") or {}

    return {
        "documentoConsultado": resultado.get("numero_consultado"),
        "tipoDocumento": resultado.get("tipo_documento"),
        "ruc": detalle.get("ruc") or seleccionado.get("ruc"),
        "razonSocial": detalle.get("razon_social") or seleccionado.get("razon_social"),
        "tipoContribuyente": detalle.get("tipo_contribuyente"),
        "estadoContribuyente": detalle.get("estado_contribuyente") or seleccionado.get("estado"),
        "condicionContribuyente": detalle.get("condicion_contribuyente"),
        "domicilioFiscal": detalle.get("domicilio_fiscal"),
        "actividadesEconomicas": detalle.get("actividades_economicas", []),
        "nombreComercial": detalle.get("nombre_comercial"),
        "fechaInscripcion": detalle.get("fecha_inscripcion"),
        "fechaInicioActividades": detalle.get("fecha_inicio_actividades"),
        "sistemaEmisionComprobante": detalle.get("sistema_emision_comprobante"),
        "sistemaContabilidad": detalle.get("sistema_contabilidad"),
        "comprobantesElectronicos": detalle.get("comprobantes_electronicos"),
        "padrones": detalle.get("padrones"),
        "fuente": "SUNAT_CONSULTA_RUC_WEB",
        "rawSunat": resultado,
    }


async def main():
    load_dotenv()

    parser = argparse.ArgumentParser(description="Consulta SUNAT por documento o RUC usando Playwright.")
    parser.add_argument("numero", help="Número de documento o RUC. Ejemplo: 73420557")
    parser.add_argument("--tipo", default="1", help="Tipo documento: 1 DNI, 4 CE, 7 Pasaporte, A Ced. Diplomática.")
    parser.add_argument("--modo", default="documento", choices=["documento", "ruc"], help="Modo de consulta.")
    parser.add_argument("--headless", default="true", choices=["true", "false"], help="Ejecutar navegador oculto o visible.")
    parser.add_argument("--slow-mo", type=int, default=0, help="Delay en ms para ver pasos. Ejemplo: 200")
    parser.add_argument("--salida", default="resultados_sunat", help="Carpeta o archivo .json de salida.")
    parser.add_argument("--backend-url", default=os.getenv("BACKEND_URL"), help="URL de tu backend para guardar.")
    parser.add_argument("--backend-token", default=os.getenv("BACKEND_TOKEN"), help="Token JWT opcional.")
    parser.add_argument("--payload-simple", action="store_true", help="Enviar/mostrar payload simple para backend.")
    args = parser.parse_args()

    try:
        resultado = await consultar_sunat(
            numero=args.numero,
            tipo_doc=args.tipo,
            modo=args.modo,
            headless=args.headless.lower() == "true",
            slow_mo=args.slow_mo,
        )

        if args.payload_simple:
            payload_backend = crear_payload_simple(resultado)
            print(json.dumps(payload_backend, ensure_ascii=False, indent=2))
        else:
            print(json.dumps(resultado, ensure_ascii=False, indent=2))

        archivo = guardar_json(resultado, args.salida)
        print(f"\nJSON guardado en: {archivo}")

        if args.backend_url:
            payload = crear_payload_simple(resultado)
            respuesta_backend = enviar_a_backend(payload, args.backend_url, args.backend_token)
            print("\nRespuesta backend:")
            print(json.dumps(respuesta_backend, ensure_ascii=False, indent=2))

            if not respuesta_backend["ok"]:
                sys.exit(2)

        if not resultado.get("ok"):
            sys.exit(1)

    except KeyboardInterrupt:
        print("Proceso cancelado por el usuario.")
        sys.exit(130)
    except Exception as e:
        print(f"ERROR: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    asyncio.run(main())
