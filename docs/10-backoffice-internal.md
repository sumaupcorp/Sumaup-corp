# 10 — Plataforma interna (Backoffice SUMAUP)

Proyecto: `sumaup360-backoffice`. Next.js + React + TS + Tailwind + shadcn/ui + TanStack
Query + Recharts (mismo stack que el SaaS, para reutilizar). Consume el **mismo backend**.

## Qué es y por qué va separado

Es la **tercera línea** del ecosistema: la plataforma **interna** que usa el personal de
SUMAUP para operar todo. NO es para clientes.

- **Cross-tenant por diseño:** ve datos de todos los tenants y de todos los usuarios de la
  app móvil. Por eso NO puede vivir dentro del SaaS de clientes (sería un riesgo de
  seguridad y rompería el modelo multi-tenant).
- **Cima del RBAC:** `Backoffice SUMAUP → controla el ERP y las licencias → Empresa(tenant)
  → Trabajadores`.
- **Acceso restringido:** solo empleados SUMAUP (idealmente red interna / acceso controlado).

## Tres identidades que NUNCA se confunden

| Identidad | Quién | Dónde entra |
|---|---|---|
| **Staff** (interno) | Empleado SUMAUP | Backoffice |
| **Usuario de la app** (Personas) | Persona natural/independiente | App móvil |
| **Trabajador de tenant** (Negocios) | Empleado de un cliente | SaaS ERP |

Todas se autentican con el **mismo Firebase**; el **backend** distingue el contexto y aplica
el RBAC que corresponde.

## Roles staff (RBAC interno)

`gerencia`, `admin` (super-admin), `contador`, `desarrollador`, `soporte`, `logistica`
(extensible). Cada rol ve módulos distintos (matriz rol×módulo en
`sumaup360-backoffice/docs/STAFF_RBAC.md`). Ejemplos:
- **contador:** comprobantes + soporte.
- **gerencia:** reportes/KPIs + licencias (lectura).
- **admin:** todo + staff/roles.
- **desarrollador:** logs/técnico. **soporte:** tickets/usuarios. **logistica:** operación.

## Funciones núcleo

1. **Procesamiento de comprobantes (intake)** — núcleo del soporte a la app.
2. **Soporte** a usuarios de la app (tickets, ver cuenta, ayudar).
3. **Gestión de licencias/membresías** del SaaS ERP (alta/baja/renovación por tenant).
4. **Supervisión cross-tenant** (empresas, planes, métricas).
5. **Administración interna** (usuarios staff y roles).
6. **Auditoría / logs.**
7. **Reportes / KPIs** (gerencia).

## Flujo de comprobantes (end-to-end)

```
App móvil: usuario sube boleta/factura/RxH (foto/PDF, sin OCR en MVP)
   └─► Backend: guarda comprobante  estado = pendiente
          └─► Backoffice: cola de intake (contador toma / se le asigna)
                 ├─ revisa, carga la información en SUMAUP360   estado = en_proceso
                 ├─ correcto → estado = procesado
                 └─ falta algo → estado = observado (con nota)
                        └─► App móvil: refleja el estado y la nota al usuario
```

Estados: `pendiente → en_proceso → procesado | observado`. La app **solo** sube y muestra
estado; el procesamiento es manual y vive aquí.

## Backend de soporte

El backend gana un módulo interno (`com.sumaup360.backoffice`) para:
- Identidades **staff** (marcadas como internas en el schema `auth`) y roles staff.
- Endpoints de **intake/procesamiento** de comprobantes (operan sobre `app.receipt`).
- Operaciones de **licenciamiento** (sobre `billing`/`subscription`), cross-tenant.

Los comprobantes en sí siguen viviendo en el schema `app` (los sube la Línea Personas); el
backoffice es el operador. El licenciamiento vive en `billing`.

## Fase

El backoffice se construye junto con el MVP de la app (Fase 7) y el billing (Fase 8), apoyado
en el backend de identidad (Fase 1). Su procesamiento de comprobantes es lo que hace usable
el "subir comprobante" de la app desde el día uno del MVP.
