# Feature: document-templates (SaaS dashboard)

Configuracion y preview de plantillas de documentos comerciales. El PDF oficial lo genera el
**backend** (`/api/v1/erp/documents/render-pdf`); el frontend solo configura, previsualiza y
descarga.

## Estructura
- `types/document-template.types.ts` — contrato con el backend (enums, request/response).
- `services/document-template.service.ts` — cliente REST (envia `Authorization: Bearer idToken`).
- `components/`
  - `DocumentTemplateList` — lista + marcar predeterminada.
  - `DocumentTemplateEditor` — tipo, formato, marca y preview en vivo; guarda.
  - `DocumentBrandingForm` — logo, colores, flags show*, footer (JSON).
  - `DocumentFormatSelector` — A4 / 80mm / 58mm.
  - `DocumentTemplatePreview` — iframe con el HTML de ejemplo del backend.
  - `DocumentSeriesForm` — crear/listar series y ver correlativo.

## Rutas
- `/dashboard/settings/document-templates?companyId=...`
- `/dashboard/settings/document-series?companyId=...`

## Estado
Skeleton funcional. Requiere inicializar el proyecto Next (`create-next-app`) y configurar
`NEXT_PUBLIC_API_BASE_URL`. Una sola plantilla base por formato; los campos por rubro son
opcionales (no se duplica por rubro). QR/hash quedan como placeholder para SUNAT.
