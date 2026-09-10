# sumaup360-backoffice — Plataforma interna (Backoffice SUMAUP)

Plataforma web **interna** de operación de SUMAUP. La usan **solo empleados de SUMAUP**
(staff), nunca clientes ni usuarios de la app móvil.

## Qué es

Es la **tercera línea** del ecosistema SUMAUP360 y la **cima del RBAC**:

> Backoffice SUMAUP → controla el ERP y las licencias → Empresa (tenant) → Trabajadores

Es **cross-tenant por diseño**: ve datos de todos los tenants y de todos los usuarios de
la app móvil (Línea Personas). Por seguridad va **separado del SaaS de clientes**
(`sumaup360-saas`), aunque comparte stack, lenguaje visual y el mismo backend.

No confundir: es **distinto** del SaaS de clientes (Línea Negocios). Aquí operan empleados
de SUMAUP, no negocios ni consumidores finales.

## Para quién

Staff SUMAUP con roles internos: gerencia, admin, contador, desarrollador, soporte, logística.

## Funciones núcleo

1. **Procesamiento de comprobantes** (núcleo): cola de boletas/facturas que los usuarios de
   la app móvil suben; los contadores las revisan, las cargan en SUMAUP360 y actualizan su
   estado (pendiente → en proceso → procesado / observado). La app refleja el estado.
2. **Soporte** a usuarios de la app móvil (tickets, ver cuenta, ayudar).
3. **Licencias / membresías** del SaaS ERP: alta/baja/renovación por tenant.
4. **Supervisión cross-tenant**: tenants, empresas, planes, métricas.
5. **Administración interna**: usuarios staff y sus roles.
6. **Auditoría / logs**.
7. **Reportes / KPIs** para gerencia.

## Stack

Next.js (App Router) · React · TypeScript · Tailwind · shadcn/ui · TanStack Query · Recharts.
Mismo stack que `sumaup360-saas` para reutilizar patrones.

## Backend (autoridad)

Consume el **mismo** `sumaup360-backend`, que es la autoridad de RBAC, tenant y membresía.
El frontend solo envía `Authorization: Bearer <idToken>` y refleja permisos; **nunca** decide
reglas ni autorización.

## Auth y acceso restringido

Login de staff con Firebase (mismo proyecto del ecosistema) → `idToken` → el backend verifica
y resuelve que es un usuario **interno** (staff) con rol staff. Firebase es compartido, pero el
backend **separa contextos** (cliente vs staff). Acceso restringido a personal autorizado.

## Comandos de init (pendientes, NO ejecutados en esta fase)

```bash
npx create-next-app@latest . --ts --tailwind --app --eslint
npx shadcn@latest init
npm i @tanstack/react-query recharts firebase
```

## Fase actual

**Skeleton**: solo estructura, contratos (tipos TS) y documentación. Sin features completas,
sin ejecutar npm/next.

## Relación con el ecosistema

- Backend autoridad: `../sumaup360-backend`.
- SaaS de clientes (no tocar, solo alinear): `../sumaup360-saas`.
- App móvil (origen de los comprobantes): `../sumaup360-mobile`.
- Docs raíz: `../docs/00-09` y `../docs/10-backoffice-internal.md`.
- Skills raíz: `../.claude/skills/`.
