# CLAUDE.md — sumaup360-backoffice (Plataforma interna / Backoffice)

Contexto técnico de este proyecto. Lee primero `../CLAUDE.md` (ecosistema) y luego este.

## Qué es

Plataforma web **interna** para staff de SUMAUP. Tercera línea del ecosistema y **cima del
RBAC**: `Backoffice → ERP/licencias → Empresa (tenant) → Trabajadores`. Es **cross-tenant**:
ve todos los tenants y todos los usuarios de la app móvil. Va **separado** del SaaS de clientes
(`sumaup360-saas`) por seguridad; comparte stack, lenguaje visual y backend.

### Tres tipos de identidad — NO confundir

- **Staff** (interno): empleados de SUMAUP que usan ESTE backoffice. Rol staff.
- **Usuarios de la app** (Línea Personas): suben comprobantes; reciben soporte. NO son staff.
- **Trabajadores de un tenant** (Línea Negocios): usan el SaaS ERP. NO son staff.

## Stack

Next.js (App Router) · React · TypeScript · Tailwind · shadcn/ui · TanStack Query · Recharts.

## Roles staff (RBAC interno, extensible)

`gerencia · admin · contador · desarrollador · soporte · logistica`. Cada rol ve módulos
distintos. La matriz rol×módulo vive en `docs/STAFF_RBAC.md` y los tipos en `src/types/staff.ts`.
El **backend** define los permisos reales; la UI solo oculta/deshabilita (UX, no seguridad).

## Flujo de comprobantes (núcleo)

App móvil sube comprobante → backend lo guarda como `pendiente` → en el backoffice un
**contador** lo revisa, lo asigna, lo carga en SUMAUP360 y cambia el estado
(`pendiente → en_proceso → procesado | observado`) → la app móvil refleja el estado.
Detalle: `docs/COMPROBANTE_INTAKE.md` y skill `backoffice-comprobante-flow`.

## Auth y RBAC (regla de oro)

- **Firebase = identidad. Backend = autoridad** de RBAC, tenant y membresía.
- Login de staff por Firebase → `idToken` → el backend resuelve contexto **staff** (≠ cliente).
- El frontend solo envía `Authorization: Bearer <idToken>` y refleja permisos. **Nunca** decide
  autorización. Ver `../docs/06-auth-security.md` y skill raíz `sumaup-auth-firebase-rbac`.

## Reglas UI (heredadas de la landing)

- Español real, sin lorem ipsum. **Sin emojis.** Sin imágenes externas (assets locales).
- Fondo blanco, **sin modo oscuro** ni theme switcher.
- Tipografía: Manrope (cuerpo/UI), Sora (titulares). Paleta azul/celeste/blanco.
- Mascota Suma (mono azul) y chatbot Suma como hilo conductor.
- Responsive mobile-first: 360 / 390 / 768 / 1280. Cero overflow horizontal.
- Toda UI que consume datos tiene estados de **carga / error / vacío**.

## Estructura

- `src/app/` — rutas App Router: `(auth)` login staff, `(backoffice)` layout con sidebar.
- `src/components/` — `layout` (sidebar/topbar), `dashboard`, `tables`, `forms`, `charts`, `ui`.
- `src/features/` — dominio: auth, comprobantes, support, app-users, licenses, tenants, plans,
  staff, roles, audit, reports.
- `src/lib/` — api client (Bearer idToken), query client TanStack, utils.
- `src/types/` — contratos TS (`staff.ts`, `comprobante.ts`).

## Referencias (no duplicar)

- Docs ecosistema: `../docs/00-09` y `../docs/10-backoffice-internal.md`.
- Skills raíz: `../.claude/skills/` (`sumaup-domain-model`, `sumaup-auth-firebase-rbac`,
  `sumaup-conventions`). Agentes/skills propios: `.claude/agents/`, `.claude/skills/`.
- SaaS de clientes (alinear tipos, no tocar): `../sumaup360-saas/src/types/domain.ts`.

## Reglas de trabajo

Primero estructura y contratos, después implementación. Nada se integra sin `build` + `lint`
verdes. No duplicar reglas de negocio: la autoridad es el backend.
