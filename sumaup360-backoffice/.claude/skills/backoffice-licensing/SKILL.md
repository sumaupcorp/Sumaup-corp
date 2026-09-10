---
name: backoffice-licensing
description: Modelo de licencias/membresías del SaaS ERP y operaciones cross-tenant. Úsalo al diseñar alta/baja/renovación de licencias, estados de membresía o la relación tenant↔plan desde el backoffice.
---

# Skill — Licencias / membresías (cross-tenant)

Cómo el backoffice gestiona qué negocio (tenant) tiene qué plan del SaaS ERP, de forma
cross-tenant.

## Conceptos

- **Tenant / Company**: cuenta de negocio (Línea Negocios). Lo supervisa `features/tenants`.
- **Plan**: techo comercial (módulos permitidos y límites). Lo gestiona `features/plans`.
- **Membresía**: vínculo vigente tenant↔plan, con estado y fechas.

## Estados de membresía

`active · trial · past_due · suspended · canceled`.

## Operaciones

- Alta: asignar un plan a un tenant.
- Renovación: extender la vigencia (`renewsAt`).
- Cambio de plan: subir/bajar de plan.
- Baja/suspensión: `canceled` / `suspended`.

## Reglas

- El backend valida cada operación; el frontend refleja y dispara vía API.
- Operación cross-tenant: el backoffice ve todos los tenants (a diferencia del SaaS de clientes).
- Cada cambio de licencia se registra en auditoría.
- RBAC: admin opera; gerencia y logística suelen tener lectura (`docs/STAFF_RBAC.md`).

## Alineación de tipos

Reutiliza conceptualmente `Plan`, `Membership`, `Tenant`, `Company` de
`../sumaup360-saas/src/types/domain.ts` sin acoplar; redefine lo mínimo localmente. Fuente de
dominio: skill raíz `sumaup-domain-model`.
