"""API Flask del microservicio SUNAT.

Usa el scraper de consultar_sunat_dni.py (Playwright, sin OCR ni APIs externas).
Expone los endpoints que consume el backend Spring:

    GET /api/sunat/dni/<dni>?doc_type=1   (1=DNI,4=CE,7=Pasaporte,A=Ced.Diplomatica)
    GET /api/sunat/ruc/<ruc>
    GET /health

Proteccion opcional: si SUNAT_API_KEY esta seteada, exige el header X-API-Key.
"""
from __future__ import annotations

import asyncio
import os
import re
from typing import Any, Dict, List, Optional

from flask import Flask, jsonify, request

from .consultar_sunat_dni import consultar_sunat
from .validar_sol import validar_sol

app = Flask(__name__)

API_KEY = os.getenv("SUNAT_API_KEY", "")
# Navegador VISIBLE por defecto (asi funciono la prueba; pasa mejor el WAF de SUNAT).
HEADLESS = os.getenv("SUNAT_PLAYWRIGHT_HEADLESS", "false").lower() == "true"

_ACT_RE = re.compile(r"(Principal|Secundaria)\s*-\s*(\d{3,5})\s*-\s*(.+)", re.IGNORECASE)
_RUC_RE = re.compile(r"^\d{11}$")
_DOC_RE = re.compile(r"^[A-Za-z0-9]{6,15}$")


def _check_api_key() -> Optional[Any]:
    if API_KEY and request.headers.get("X-API-Key", "") != API_KEY:
        return jsonify({"detail": "API key invalida"}), 401
    return None


def _parse_activities(items: List[str]) -> List[Dict[str, Optional[str]]]:
    acts: List[Dict[str, Optional[str]]] = []
    seen = set()
    for raw in items or []:
        m = _ACT_RE.search(raw or "")
        if not m:
            continue
        tipo, ciiu, desc = m.group(1).title(), m.group(2), m.group(3).strip()
        if (tipo, ciiu) in seen:
            continue
        seen.add((tipo, ciiu))
        acts.append({"tipo": tipo, "ciiu": ciiu, "descripcion": desc})
    return acts


def _normalize(result: Dict[str, Any]) -> Dict[str, Any]:
    """Adapta la respuesta del scraper al contrato que parsea el backend Spring."""
    det = result.get("detalle") or {}
    acts = _parse_activities(det.get("actividades_economicas") or [])
    principal = next((a for a in acts if (a["tipo"] or "").lower() == "principal"),
                     acts[0] if acts else None)
    return {
        "ruc": det.get("ruc") or "",
        "razon_social": det.get("razon_social"),
        "estado": det.get("estado_contribuyente"),
        "condicion": det.get("condicion_contribuyente"),
        "tipo_contribuyente": det.get("tipo_contribuyente"),
        "direccion": det.get("domicilio_fiscal"),
        "actividad_principal": principal,
        "actividades": acts,
        "source": "sunat-scraper",
        "completo": bool(det.get("tipo_contribuyente") and principal),
        # Datos extra de la ficha por si el backend/diagnostico los quiere a futuro.
        "raw": det,
    }


def _run(coro):
    return asyncio.run(coro)


def _error_response(result: Dict[str, Any]):
    """404 si el documento no tiene RUC (caso valido de negocio); 502 si fallo real."""
    detail = result.get("mensaje", "No se pudo consultar SUNAT.")
    status = 404 if result.get("sin_ruc") else 502
    return jsonify({"detail": detail, "sin_ruc": bool(result.get("sin_ruc"))}), status


@app.get("/health")
def health():
    return jsonify({"status": "ok", "headless": HEADLESS})


@app.get("/api/sunat/dni/<dni>")
def consultar_dni(dni: str):
    guard = _check_api_key()
    if guard:
        return guard
    if not _DOC_RE.match(dni):
        return jsonify({"detail": "Numero de documento invalido."}), 422
    doc_type = request.args.get("doc_type", "1")
    try:
        result = _run(consultar_sunat(numero=dni, tipo_doc=doc_type, modo="documento", headless=HEADLESS))
    except Exception as e:  # noqa: BLE001
        return jsonify({"detail": f"Error consultando SUNAT: {e}"}), 502
    if not result.get("ok"):
        return _error_response(result)
    return jsonify(_normalize(result))


@app.get("/api/sunat/ruc/<ruc>")
def consultar_ruc(ruc: str):
    guard = _check_api_key()
    if guard:
        return guard
    if not _RUC_RE.match(ruc):
        return jsonify({"detail": "El RUC debe tener 11 digitos."}), 422
    try:
        result = _run(consultar_sunat(numero=ruc, modo="ruc", headless=HEADLESS))
    except Exception as e:  # noqa: BLE001
        return jsonify({"detail": f"Error consultando SUNAT: {e}"}), 502
    if not result.get("ok"):
        return _error_response(result)
    return jsonify(_normalize(result))


@app.post("/api/sunat/validar-sol")
def validar_sol_endpoint():
    """Valida la Clave SOL iniciando sesion en el Menu SOL. No guarda nada.

    Body JSON:
        {"mode": "dni", "dni": "########", "clave": "..."}
        {"mode": "ruc", "ruc": "###########", "usuario": "XXXX", "clave": "..."}

    Respuesta 200: {"ok": true, "nombre": "..."} | {"ok": false, "detail": "..."}
    """
    guard = _check_api_key()
    if guard:
        return guard
    body = request.get_json(silent=True) or {}
    mode = str(body.get("mode", "")).lower()
    clave = (body.get("clave") or "").strip()
    dni = (body.get("dni") or "").strip()
    ruc = (body.get("ruc") or "").strip()
    usuario = (body.get("usuario") or "").strip()

    if mode not in ("dni", "ruc"):
        return jsonify({"ok": False, "detail": "Modo invalido (dni o ruc)."}), 422
    if not clave:
        return jsonify({"ok": False, "detail": "Falta la contrasena."}), 422
    if mode == "dni" and not re.fullmatch(r"\d{8}", dni):
        return jsonify({"ok": False, "detail": "DNI invalido (8 digitos)."}), 422
    if mode == "ruc" and (not _RUC_RE.match(ruc) or not usuario):
        return jsonify({"ok": False, "detail": "Ingresa RUC (11 digitos) y usuario SOL."}), 422

    try:
        result = _run(validar_sol(
            mode=mode, clave=clave, dni=dni, ruc=ruc, usuario=usuario, headless=HEADLESS,
        ))
    except Exception as e:  # noqa: BLE001
        return jsonify({"ok": False, "detail": f"Error validando en SUNAT: {e}"}), 502
    return jsonify(result)
