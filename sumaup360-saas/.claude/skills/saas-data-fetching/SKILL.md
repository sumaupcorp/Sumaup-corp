---
name: saas-data-fetching
description: Patrón de obtención de datos del dashboard con TanStack Query y un api client que envía Authorization Bearer idToken. Úsalo al crear hooks de query/mutación, manejar caché, errores 401/403 y estados de carga/error/vacío.
---

# Skill: data fetching (TanStack Query + Bearer idToken)

Patrón único para hablar con el backend: tipado, cacheado y con el idToken de Firebase.

## Api client

- Interceptor que adjunta `Authorization: Bearer <idToken>` en cada request.
- Refresca el idToken antes de expirar; normaliza errores a una forma común.
- Solo transporta: **no** decide autorización ni reglas de negocio.

## TanStack Query

- `QueryClient` con defaults razonables (staleTime, retry, refetch).
- **Query keys** estables y jerárquicas por feature/recurso/tenant, ej.:
  `["sales", tenantId, branchId, filters]`.
- Queries para lectura; mutaciones con invalidación de las keys afectadas.

## Estados de UI

- Modelar **carga / error / vacío** con `isLoading`, `isError`, datos vacíos. Sin pantallas en
  blanco ni spinners infinitos.

## Manejo de errores

- **401** → reautenticar con Firebase y reintentar / redirigir a login.
- **403** → sin permiso: reflejar en UI (coordinar con `saas-rbac-gating`).
- Otros → mensaje claro en español y opción de reintento.

## Reglas

- Nunca commitear credenciales/service accounts (ver `.gitignore`).
- Tipos alineados con `src/types/domain.ts`; idealmente generados desde el OpenAPI del backend.

## Referencias

`src/lib/README.md`, `../../docs/06-auth-security.md`, agente `saas-data-layer`.
