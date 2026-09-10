# sumaup360-saas — Dashboard SaaS / ERP (Línea Negocios)

Dashboard web multi-tenant de SUMAUP360 para administrar negocios: empresa → sucursales →
trabajadores, con inventario, ventas, caja/POS, comprobantes y reportes. **Un solo dashboard
que se configura por rubro** (no un dashboard por rubro).

## Qué es

- Frontend de la **Línea Negocios** del ecosistema SUMAUP360.
- Refleja en la UI los `enabledModules`, el `plan`, la `membership` y los `permissions` que
  entrega el backend. **No** decide reglas de negocio ni autorización: eso vive en el backend.
- Identidad con Firebase; el backend Spring es la autoridad de RBAC, tenant y membresía.

## Stack

- Next.js (App Router), React, TypeScript.
- Tailwind CSS + shadcn/ui (lenguaje visual heredado de la landing `sumaup360-reborn`).
- TanStack Query (data fetching/caché) + Recharts (KPIs y reportes).

## Estado

Fase: **skeleton** (estructura, contratos y documentación). Aún no se implementan features
completas ni se ha ejecutado `npm`/`next`. Carpetas con README describen su propósito.

## Cómo se inicializará (pendiente, no ejecutado aún)

```
npx create-next-app@latest . --ts --tailwind --app --src-dir --eslint
npx shadcn@latest init
npm i @tanstack/react-query recharts firebase
```

Tras inicializar: `npm run dev`, `npm run build`, `npm run lint`.

## Relación con el backend

- Toda llamada va con `Authorization: Bearer <idToken>` (ver `src/lib/README.md`).
- El menú/navegación se construye desde `enabledModules` (ver `docs/MODULE_SYSTEM.md`).
- Contrato de dominio en `src/types/domain.ts`; tipos idealmente generados desde el OpenAPI
  del backend cuando exista.

## Documentación relacionada

- Ecosistema: `../docs/00-09` y `../CLAUDE.md`.
- Skills raíz: `../.claude/skills/` (`sumaup-domain-model`, `sumaup-auth-firebase-rbac`,
  `sumaup-conventions`).
- Skills y agentes propios: `.claude/`.
