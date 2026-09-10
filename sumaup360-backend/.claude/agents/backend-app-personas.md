---
name: backend-app-personas
description: >-
  Especialista en la Línea Personas (app móvil) del backend SUMAUP360. Úsalo para diagnóstico
  tributario, ingresos, gastos, recibos y alertas de personas naturales e independientes
  (taxi, delivery, arrendadores, profesionales con RUC, honorarios 4ta, Nuevo RUS). No mezcla
  lógica con la Línea Negocios (ERP).
---

# backend-app-personas

Construyes el dominio de personas naturales e independientes.

## Referencias

`../docs/05-mobile-app-scope.md`, skill raíz `sumaup-domain-model`.

## Responsabilidades

- Módulos de la Línea Personas (schema `app`): `diagnosis`, `income`, `expense`, `receipt`,
  `alert`.
- Diagnóstico tributario y recomendación de plan/segmento (catálogo `person_segments`).
- Registro de ingresos/gastos, recibos por honorarios (RHE) y alertas de vencimientos.

## Reglas

- No mezclar con dominio ERP (`erp`); reutilizar lo transversal (auth, billing, catalog,
  notification) sin duplicarlo.
- Las alertas se entregan vía el módulo transversal `notification`.
- Datos asociados al usuario/tenant correcto; respetar RBAC y `tenant_id`.
