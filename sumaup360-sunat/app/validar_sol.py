#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Validacion de Clave SOL en SUNAT usando Playwright.

Inicia sesion en el Menu SOL con las credenciales de la persona para comprobar que
son correctas. NO guarda nada: solo responde si el login funciono y el nombre del
titular (para mostrar confianza). Soporta los dos modos de ingreso de SUNAT:

    - Por DNI:  DNI (8) + Contrasena
    - Por RUC:  RUC (11) + Usuario SOL (8) + Contrasena

Devuelve un dict: {"ok": bool, "nombre": str|None, "detail": str|None}

Uso directo (debug):
    python -c "import asyncio; from app.validar_sol import validar_sol; \
               print(asyncio.run(validar_sol(mode='ruc', ruc='...', usuario='...', clave='...')))"
"""
from __future__ import annotations

import os
from typing import Any, Dict, Optional

from playwright.async_api import async_playwright, TimeoutError as PlaywrightTimeoutError

# URL de login del Menu SOL. El client id es el de la app "Menu SOL" de SUNAT (estable).
# No fijamos `state` (es de un solo uso): esta URL renderiza el formulario y SUNAT
# completa el OAuth tras enviar las credenciales. Se puede sobreescribir por entorno.
DEFAULT_LOGIN_URL = (
    "https://api-seguridad.sunat.gob.pe/v1/clientessol/"
    "59d39217-c025-4de5-b342-393b0f4630ab/oauth2/loginMenuSol"
    "?lang=es-PE&showDni=true&showLanguages=false"
    "&originalUrl=https://e-menu.sunat.gob.pe/cl-ti-itmenu2/AutenticaMenuInternetPlataforma.htm"
)
LOGIN_URL = os.getenv("SUNAT_SOL_LOGIN_URL", DEFAULT_LOGIN_URL)


async def _fill_visible(page, selector: str, value: str) -> None:
    """Llena el primer elemento VISIBLE que matchea (SUNAT duplica ids entre pestanas)."""
    loc = page.locator(f"{selector}:visible").first
    await loc.wait_for(state="visible", timeout=15000)
    await loc.fill(value)


async def _click_optional(page, selector: str) -> None:
    """Click en la pestana (DNI/RUC) si existe; ignora si ya esta activa o no aparece."""
    loc = page.locator(selector)
    try:
        if await loc.count() > 0:
            await loc.first.click(timeout=5000)
    except Exception:  # noqa: BLE001
        pass


async def validar_sol(
    *,
    mode: str,
    clave: str,
    dni: Optional[str] = None,
    ruc: Optional[str] = None,
    usuario: Optional[str] = None,
    headless: bool = False,
    slow_mo: int = 0,
) -> Dict[str, Any]:
    mode = (mode or "").lower()
    if mode not in ("dni", "ruc"):
        return {"ok": False, "nombre": None, "detail": "Modo invalido (usa dni o ruc)."}
    if not clave:
        return {"ok": False, "nombre": None, "detail": "Falta la contrasena."}

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
            await page.goto(LOGIN_URL, wait_until="domcontentloaded", timeout=45000)

            if mode == "dni":
                await _click_optional(page, "#btnPorDni")
                await _fill_visible(page, "#txtDni", dni or "")
            else:
                await _click_optional(page, "#btnPorRuc")
                await _fill_visible(page, "#txtRuc", ruc or "")
                await _fill_visible(page, "#txtUsuario", usuario or "")

            await _fill_visible(page, "#txtContrasena", clave)
            await page.locator("#btnAceptar:visible").first.click()

            # Espera un desenlace claro: bienvenida (ok) o mensaje de error de SUNAT.
            try:
                await page.wait_for_function(
                    """
                    () => {
                        const w = document.querySelector('#aOpcionUsuario2');
                        const okEl = w && /Bienvenido/i.test(w.innerText || '');
                        const okTxt = (document.body ? document.body.innerText : '').includes('Bienvenido,');
                        const err = document.querySelector('#lblErrorDni, #lblErrorRuc, span[id^="lblError"]');
                        const hasErr = err && (err.innerText || '').trim().length > 0;
                        return okEl || okTxt || hasErr;
                    }
                    """,
                    timeout=25000,
                )
            except PlaywrightTimeoutError:
                return {
                    "ok": False,
                    "nombre": None,
                    "detail": "SUNAT no respondio a tiempo. Intenta de nuevo en un momento.",
                }

            res = await page.evaluate(
                """
                () => {
                    const w = document.querySelector('#aOpcionUsuario2');
                    let nombre = null;
                    if (w) {
                        for (const s of w.querySelectorAll('span')) {
                            const t = (s.innerText || '').trim();
                            if (t && !/Bienvenido/i.test(t)) { nombre = t; break; }
                        }
                    }
                    const okTxt = (document.body ? document.body.innerText : '').includes('Bienvenido,');
                    const err = document.querySelector('#lblErrorDni, #lblErrorRuc, span[id^="lblError"]');
                    const errMsg = err ? (err.innerText || '').trim() : '';
                    return { ok: !!(nombre || okTxt), nombre, errMsg };
                }
                """
            )
            if res.get("ok"):
                return {"ok": True, "nombre": res.get("nombre"), "detail": None}
            detail = res.get("errMsg") or (
                "RUC, usuario y/o contrasena incorrectos. Verifica tus datos."
            )
            return {"ok": False, "nombre": None, "detail": detail}
        finally:
            await context.close()
            await browser.close()
