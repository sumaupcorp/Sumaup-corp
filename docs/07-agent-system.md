# 07 — Sistema de agentes

SUMAUP360 usa una jerarquía de agentes de Claude: **padre → transversales → arquitecto
sub-padre por proyecto → sub-agentes especialistas + skills**.

## Jerarquía

```
sumaup360-master-architect  (PADRE — raíz .claude/agents)
│
├─ Transversales (raíz .claude/agents)
│   ├─ product-domain-architect   (rubros, segmentos, planes, membresías, RBAC de dominio)
│   ├─ postgres-data-architect     (schemas, multi-tenant, Flyway, índices)
│   └─ qa-release-architect        (pruebas, builds, checklist de entrega)
│
├─ Landing — sumaup360-reborn/.claude/agents  (YA EXISTE)
│   └─ product-architect · ui-pro-max · suma-assets-director · frontend-builder · qa-web-tester
│
├─ Backend — sumaup360-backend/.claude/agents
│   ├─ backend-spring-architect   (sub-padre)
│   ├─ backend-auth-security · backend-multitenant-rbac · backend-data-flyway
│   ├─ backend-erp-modules · backend-app-personas · backend-billing · backend-qa
│
├─ SaaS — sumaup360-saas/.claude/agents
│   ├─ saas-dashboard-architect   (sub-padre)
│   ├─ saas-module-system · saas-rbac-ui · saas-data-layer · saas-ui-builder · saas-qa
│
├─ Mobile — sumaup360-mobile/.claude/agents
│   ├─ flutter-app-architect       (sub-padre)
│   ├─ flutter-auth · flutter-feature-builder · flutter-data · flutter-ui · flutter-qa
│
└─ Backoffice (interno) — sumaup360-backoffice/.claude/agents
    ├─ backoffice-architect        (sub-padre)
    ├─ backoffice-comprobantes · backoffice-staff-rbac · backoffice-licensing
    ├─ backoffice-data-layer · backoffice-ui-builder · backoffice-qa
```

> El backend incluye además `backend-backoffice` para dar soporte a esta línea interna
> (staff, intake de comprobantes, licenciamiento cross-tenant).

## Skills

- **Raíz** (`.claude/skills/`): `sumaup-domain-model`, `sumaup-auth-firebase-rbac`,
  `sumaup-conventions`. Aplican a todo el ecosistema.
- **Por proyecto**: cada carpeta tiene sus skills técnicas (blueprints, patrones, design
  system). Ver el README de cada proyecto.

## Cómo trabajan juntos

1. El **master architect** recibe el objetivo, lo divide por dominios y delega al arquitecto
   sub-padre del proyecto correspondiente, citando los docs y skills relevantes.
2. El **sub-padre** del proyecto planifica dentro de su stack y reparte a sus sub-agentes.
3. Los **transversales** se consultan cuando la tarea toca dominio, datos o calidad.
4. Cualquier cambio de contrato cruzado vuelve al master architect para revisar impacto.

## Reglas

- Un agente no inventa reglas de negocio: las toma de `sumaup-domain-model` y los `docs/`.
- El padre orquesta y no acapara la implementación.
- Las decisiones selladas (auth, multi-tenant, rubros por config, polyrepo) no se cambian sin
  pasar por el padre y actualizar `docs/`.

## Por qué esta jerarquía

- **Consistencia:** una sola fuente de dominio y auth para todo el ecosistema.
- **Aislamiento:** cada proyecto tiene expertos que conocen su stack a fondo.
- **Escala ordenada:** agregar un rubro o una feature no rompe a los demás proyectos.
