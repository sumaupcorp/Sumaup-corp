# src/lib — Infraestructura compartida

- **api client**: fetch/axios con interceptor que añade `Authorization: Bearer <idToken>` de
  Firebase a cada request. El backend es la autoridad.
- **query client**: configuración de TanStack Query (caché, reintentos, invalidación).
- **utils**: helpers de formato (soles PEN, fechas), constantes y mapeos de estado.

Ver skill `backoffice-data-layer`.
