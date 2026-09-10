# src/lib — Infraestructura compartida

Utilidades transversales del dashboard. Sin reglas de negocio (esas viven en el backend).

## Contenido previsto

- **api-client** — cliente HTTP con interceptor que adjunta `Authorization: Bearer <idToken>`
  (idToken de Firebase) en cada request, maneja refresh del token y normaliza errores. Nunca
  decide autorización; solo transporta.
- **query-client** — configuración de TanStack Query (QueryClient, defaults de caché/retry,
  keys). Patrón en skill `saas-data-fetching`.
- **module-registry.ts** — registro de módulos del ERP para construir el menú desde
  `enabledModules`. Ver `../../docs/MODULE_SYSTEM.md`.
- **utils** — helpers de formato (moneda PEN, fechas), `cn` para clases, constantes.

## Reglas

- El idToken se obtiene de Firebase y se envía en cada llamada; el backend lo verifica.
- Estados de carga/error/vacío se modelan con los estados de TanStack Query en cada feature.
