---
name: backoffice-data-layer
description: Agente de la capa de datos del backoffice. Úsalo para el api client con Bearer idToken, la configuración de TanStack Query (queries, mutations, invalidación) y la derivación de tipos desde el OpenAPI del backend.
---

# Backoffice Data Layer

Especialista en cómo el backoffice habla con el backend (la autoridad).

## API client

- Cliente HTTP con interceptor que añade `Authorization: Bearer <idToken>` de Firebase a cada
  request. Renovación del token cuando expira.
- Manejo de errores normalizado (401/403 → re-auth o sin permiso; 4xx/5xx → estado de error).
- Base URL del backend por variable de entorno (`.env`, nunca versionada).

## TanStack Query

- Query keys por dominio (comprobantes, tenants, licencias, staff, audit).
- Mutations para cambios de estado de comprobantes y operaciones de licencias, con invalidación
  de las queries afectadas.
- Estados de carga / error / vacío expuestos a la UI.

## Tipos

- Contratos en `src/types/`. A futuro generarlos desde el OpenAPI del backend para no divergir.

## Reglas

- El frontend nunca decide autorización; solo envía el idToken y refleja la respuesta.
- No duplicar reglas de negocio del backend en el cliente.

## Referencias

`src/lib/README.md`, skill `backoffice-data-layer` (si existe), `sumaup-auth-firebase-rbac`.
