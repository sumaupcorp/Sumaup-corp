# sumaup360-sunat

Microservicio (Python / **Flask**) que consulta la **Ficha RUC de SUNAT** con **Playwright**
(navegador real, sin OCR ni APIs externas) y la devuelve normalizada para enriquecer el
perfil del usuario (Línea Personas) y alimentar el **diagnóstico con IA (Gemini)**.

Motor de scraping: `app/consultar_sunat_dni.py` (consulta por **DNI** —flujo usual: la
persona no recuerda su RUC— o por RUC; click en el resultado → ficha completa). La capa
Flask (`app/server.py`) lo expone como API.

## Endpoints

- `GET /api/sunat/dni/<dni>?doc_type=1` → consulta por documento.
  `doc_type`: 1=DNI, 4=Carnet Extranjería, 7=Pasaporte, A=Ced. Diplomática.
- `GET /api/sunat/ruc/<ruc>` → consulta por RUC.
- `GET /health`

Header opcional `X-API-Key` (si configuras `SUNAT_API_KEY`).

### Respuesta (normalizada)
```json
{
  "ruc": "10734205571",
  "razon_social": "ROMERO CACHA RICHAR VARONI",
  "estado": "ACTIVO",
  "condicion": "HABIDO",
  "tipo_contribuyente": "PERSONA NATURAL CON NEGOCIO",
  "direccion": "-",
  "actividad_principal": { "tipo": "Principal", "ciiu": "6201", "descripcion": "PROGRAMACIÓN INFORMÁTICA" },
  "actividades": [ ... ],
  "completo": true,
  "raw": { ...todos los campos de la ficha (nombre comercial, comprobantes electrónicos, padrones, etc.)... }
}
```

## Correr local (Windows)

```bash
cd sumaup360-sunat
pip install -r requirements.txt
python -m playwright install chromium
copy .env.example .env
python run.py            # Flask en http://localhost:8090
```

Prueba directa del scraper (CLI):
```bash
python app/consultar_sunat_dni.py 73420557 --headless false
python app/consultar_sunat_dni.py 10734205571 --modo ruc
```

**Navegador VISIBLE por defecto** (`SUNAT_PLAYWRIGHT_HEADLESS=false`): SUNAT usa un WAF
(F5) que bloquea navegadores ocultos desde IPs de datacenter. En una PC residencial el
modo visible pasa sin problema y **no requiere captcha/OCR**.

## Docker

```bash
docker build -t sumaup360-sunat .
docker run -p 8090:8090 --env-file .env sumaup360-sunat
```

## Integración

```
App móvil → Backend Spring → (HTTP X-API-Key) → sumaup360-sunat (Flask + Playwright)
                  │ guarda en app.person_profile (V22)
                  ▼
            Diagnóstico IA (Gemini) usa estado/condición/tipo/actividad
```

El backend Spring ya consume `/api/sunat/dni/{dni}` y `/api/sunat/ruc/{ruc}` y guarda los
campos en el perfil (endpoints `POST /api/v1/app/fiscal/dni-refresh` y `/ruc-refresh`).
