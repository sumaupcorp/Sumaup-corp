---
name: sumaup-domain-model
description: Modelo de dominio vivo de SUMAUP360 — líneas de producto (Personas vs Negocios), rubros/sub-rubros, segmentos, módulos habilitados, planes y membresías, y RBAC. Úsalo siempre que modeles datos, definas endpoints, construyas UI por rubro o decidas qué es transversal vs propio de una línea.
---

# SUMAUP360 — Modelo de dominio

Fuente de verdad del lenguaje de negocio. Si el código y este documento difieren, gana el
diseño acordado aquí (y se actualiza este archivo).

## Tres líneas (no se mezclan)

| | Línea Personas (App móvil) | Línea Negocios (SaaS / ERP) | Línea Interna (Backoffice) |
|---|---|---|---|
| Usuario | Persona natural / independiente | Empresa con sucursales y trabajadores | Personal SUMAUP (staff) |
| Ejemplos | Taxista, repartidor delivery, arrendador, profesional con RUC, 4ta categoría, Nuevo RUS | Botica, farmacia, ferretería, restaurante, bodega, minimarket, librería, belleza, lavandería | Gerencia, admin, contador, desarrollador, soporte, logística |
| Foco | Orden tributario personal, ingresos/gastos, alertas, plan | Operación del negocio: inventario, ventas, caja/POS | Operar todo: procesar comprobantes, soporte, licencias |
| Tenancy | Cuenta personal | Multi-tenant (empresa → sucursales → trabajadores) | Cross-tenant (ve todo) |

**Tres identidades que nunca se confunden:** staff (interno) ≠ usuario de la app (Personas)
≠ trabajador de tenant (Negocios). Todas usan el mismo Firebase; el backend resuelve el
contexto. El Backoffice es la **cima del RBAC**. Detalle en `docs/10-backoffice-internal.md`.

## Transversal (compartido por ambas líneas)

`auth`, `users`, `roles`, `permissions`, `billing`, `notifications`, `audit`, `chatbot`,
`catalog`. Vive en el backend; nunca se duplica por línea ni por rubro.

## Negocios: rubros, verticales y sub-rubros

- `businessType` (rubro) — ej.: `restaurant`, `pharmacy`, `hardware`, `grocery`,
  `minimarket`, `bookstore`, `beauty`, `laundry`.
- `vertical` / sub-rubro — especialización dentro de un rubro. Ej.: `restaurant` →
  `cevicheria`, `polleria`, `chifa`, `pasteleria`, `cafeteria`.
- **Un solo ERP core.** Cada rubro/sub-rubro = catálogos + **módulos habilitados** +
  configuración. Restaurantes añade mesas/comandas/cocina como módulos, no como app aparte.

### Módulos del ERP (ejemplos)
`company`, `branch`, `customer`, `product`, `inventory`, `sale`, `pos`, `invoice`,
`supplier`, `report`. Verticales activan extras (ej. `tables`, `kitchen-orders` para
restaurantes).

### Concepto de habilitación
```
empresa(tenant) → businessType + vertical → módulos por defecto
                → plan → módulos permitidos por plan
                → enabledModules = intersección/ajuste configurable
```

## Personas: segmentos

`taxi`, `delivery`, `landlord`, `professional-ruc`, `honorarios-4ta`, `nuevo-rus`.
Cada segmento define: diagnóstico inicial, ingresos/gastos típicos, alertas y plan
recomendado. La app no implementa lógica de ERP.

## Planes y membresías

- **Plan** = paquete comercial (por línea) que define módulos permitidos y límites
  (sucursales, usuarios, transacciones, addons).
- **Membresía** = vínculo de una cuenta/empresa con un plan vigente (estado, fechas, ciclo).
- **Addons** ej.: AI-CRM en el SaaS.

## RBAC (jerárquico)

```
Backoffice (SUMAUP)  →  controla el ERP y los planes/licencias  (roles staff)
   └─ Empresa (tenant)  →  controla sus sucursales, usuarios y módulos
        └─ Trabajadores  →  roles (ej. admin, cajero, almacén) con permisos finos
```

- **Roles staff** (Backoffice): `gerencia`, `admin`, `contador`, `desarrollador`, `soporte`,
  `logistica`. Operan cross-tenant; no son trabajadores de ningún tenant.
- **Roles** agrupan **permisos**; los permisos son acciones sobre recursos (ej.
  `sale:create`, `inventory:adjust`, `receipt:process`, `license:manage`).

## Flujo de comprobantes (intake)

La app móvil sube comprobantes (foto/PDF, sin OCR en MVP) → backend los guarda `pendiente`
→ los **contadores en el Backoffice** los procesan (`en_proceso`) y los cargan en SUMAUP360
→ `procesado` u `observado` (con nota) → la app refleja el estado. El procesamiento es
manual e interno; la app solo sube y muestra estado.
- El **backend es la autoridad** del RBAC; ver skill `sumaup-auth-firebase-rbac`.
- Todo recurso de Negocios se evalúa por `tenant_id` + permiso + (a veces) `branch_id`.

## Reglas que no se rompen

1. Personas y Negocios no comparten lógica de dominio (solo lo transversal).
2. Los rubros se configuran, no se programan por separado.
3. El backend manda en roles, permisos, tenant y membresía.
