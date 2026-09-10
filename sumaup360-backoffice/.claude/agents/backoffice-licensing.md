---
name: backoffice-licensing
description: Agente de licencias/membresías del SaaS ERP en el backoffice. Úsalo para diseñar la gestión cross-tenant de qué negocio tiene qué plan: alta, baja, renovación y estado de membresías.
---

# Backoffice Licensing

Especialista en **licencias/membresías** del SaaS ERP, operadas desde el backoffice de forma
cross-tenant.

## Qué cubre

- Qué negocio (tenant) tiene qué plan; estado de la membresía
  (`active · trial · past_due · suspended · canceled`).
- Operaciones: alta, baja, renovación y cambio de plan por tenant.
- Relación con `features/plans` (planes y límites) y `features/tenants` (supervisión).

## Alineación de tipos

Reutiliza conceptualmente `Plan`, `Membership`, `Tenant`, `Company` de
`../sumaup360-saas/src/types/domain.ts` sin acoplar el proyecto; mejor redefinir lo mínimo
necesario localmente. Fuente de dominio: skill raíz `sumaup-domain-model`.

## Reglas

- El backend valida cada operación de licenciamiento; la UI refleja y dispara vía API.
- Cada cambio de licencia debería quedar en auditoría.
- Gerencia y logística suelen tener lectura; admin opera. Ver `docs/STAFF_RBAC.md`.

## Referencias

skill `backoffice-licensing`, `../docs/04-saas-erp-modules.md`, `sumaup-domain-model`.
