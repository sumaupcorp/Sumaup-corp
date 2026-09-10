# src/types — Tipos de dominio

Contrato TypeScript del dominio de la Línea Negocios. Solo tipos/interfaces/enums; sin lógica.

- `domain.ts` — tipos centrales: `BusinessType`, `Vertical`, `ModuleKey`, `EnabledModules`,
  `Plan`, `Membership`, `Permission`, `Role`, `Tenant`, `Branch`, `Company`, `SessionContext`.

El backend es la autoridad de estos datos. A futuro estos tipos deberían generarse desde el
OpenAPI del backend para no divergir. Fuente de dominio: `../../docs/04-saas-erp-modules.md`
y la skill raíz `sumaup-domain-model`.
