# CLAUDE.md — sumaup360-sunat

Microservicio Python (FastAPI) de consulta de **Ficha RUC de SUNAT**. Lee primero el
`CLAUDE.md` raíz (`../CLAUDE.md`).

## Qué es

Módulo aparte (no vive en el monolito Spring) que **consulta y normaliza** los datos de un
RUC en SUNAT y los expone como API para que el backend los guarde en el perfil del usuario
(Línea Personas) y alimenten el diagnóstico con IA (Gemini).

## Stack

Python 3.11+, **Flask**, **Playwright** (Chromium). Sin OCR ni APIs externas.

## Arquitectura

- `app/consultar_sunat_dni.py` — **motor de scraping** (Playwright): consulta por DNI
  (default) o RUC en `FrameCriterioBusquedaWeb.jsp`; click en el resultado → ficha
  completa; parsea con JS en la página (`page.evaluate`). NO usa captcha/OCR.
- `app/server.py` — **API Flask**: `/api/sunat/dni/<dni>`, `/api/sunat/ruc/<ruc>`,
  `/health`. Normaliza la salida al contrato que parsea el backend.
- `run.py` — arranca Flask (puerto SUNAT_PORT, default 8090, threaded=False).

## Reglas

- Español real, sin emojis. Secrets fuera del repo (`.env`).
- La consulta RUC es **dato público**.
- **Navegador VISIBLE por defecto** (WAF F5 de SUNAT bloquea headless desde datacenter).
- Si SUNAT cambia el HTML, ajustar selectores/JS en `consultar_sunat_dni.py`.
- Proteger con `SUNAT_API_KEY` (header `X-API-Key`) que envía el backend Spring.

## Integración

El backend Spring llama a este servicio, guarda los campos en `app.person_profile` (campos
fiscales V22 pendiente) y el diagnóstico Gemini los usa. Ver README.
