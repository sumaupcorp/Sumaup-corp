# 03 — Diseño de base de datos

Motor: **PostgreSQL**. Base única: **`sumaup360_db`**. Todo el esquema lo crea el backend con
**Flyway**. Nunca se modifica el esquema a mano.

## Schemas por dominio

| Schema | Contenido | Línea |
|---|---|---|
| `auth` | identidades internas, vínculo `firebase_uid`, refresh/sesión, espejo de claims | Transversal |
| `tenant` | tenants, empresas, sucursales, usuarios-de-tenant, roles, permisos, asignaciones | Negocios |
| `app` | perfiles, diagnóstico, ingresos, gastos, recibos, alertas | Personas |
| `erp` | business-type, módulos, clientes, productos, inventario, ventas, POS, comprobantes, proveedores | Negocios |
| `billing` | planes, suscripciones, membresías, pagos | Transversal |
| `catalog` | ubigeo, monedas, unidades, tipos de comprobante, rubros, módulos, segmentos | Transversal |
| `audit` | bitácora de auditoría | Transversal |
| `chatbot` | conversaciones y contexto de Suma | Transversal |
| `notification` | plantillas, envíos, preferencias | Transversal |

## Multi-tenant

- Estrategia: **discriminador por columna `tenant_id`** (no schema-por-tenant en esta fase).
- Toda tabla de dominio multi-tenant (mayoría de `tenant`, `erp`, parte de `billing`) lleva
  `tenant_id NOT NULL` indexado.
- El backend fuerza el filtro por `tenant_id` en toda consulta; el RBAC nunca sustituye al
  aislamiento de tenant.
- Migración futura posible a esquema-por-tenant o RLS de Postgres si la escala lo exige
  (decisión diferida, documentar antes de cambiar).

## Convenciones

- Nombres en `snake_case`. Tablas en plural (`products`, `sales`), columnas claras.
- PK: definir UNA política (UUID v7 o `bigint generated always as identity`) y mantenerla.
- FKs explícitas con `ON DELETE` pensado; sin huérfanos.
- Timestamps `created_at` / `updated_at` en tablas mutables; `created_by` donde aplique.
- Índices: `tenant_id` siempre; compuestos para filtros reales (ej.
  `(tenant_id, branch_id, created_at)` en ventas).

## Migraciones Flyway

- `V1__create_schemas.sql` — crea los 9 schemas.
- `V2__initial_catalogs.sql` — catálogos mínimos (monedas, tipos de comprobante, rubros base,
  módulos base, segmentos de personas).
- `V38__rubros_veterinaria_panaderia_pasteleria.sql` — rubros `veterinary`/`bakery`/`pastry`,
  módulos `patients`/`custom-orders`, defaults por rubro (minimarket += `batch-expiry`),
  desactiva el vertical `restaurant/pasteleria`.
- `V39__erp_batch_patient_appointment_customorder_prescription.sql` — dominio genérico por
  módulo: `erp.product_batch`, `erp.patient`, `erp.appointment`, `erp.custom_order`,
  `erp.prescription` + permisos (`batch|patient|appointment|custom-order|prescription:read|manage`).
- `V40__erp_booking_page.sql` — `erp.booking_page` (reserva de citas online por QR con
  formulario dinámico `form_config` JSON) y `appointment.form_data/source`.
- `V54__lodging_module.sql` — rubro `lodging` (hotel, hostal, apart-hotel, albergue,
  hospedaje) y módulo `lodging`: `erp.room_type`, `erp.room`, `erp.stay`, `erp.stay_guest`
  (registro legal de huéspedes) y `erp.stay_charge`; `sale_item.product_id` pasa a opcional
  con `description` para líneas libres; permisos `lodging:read|manage`. TDR:
  `11-rubro-hospedaje.md`.
- `V55__lodging_hourly.sql` — alquiler por horas: `room_type.rate_per_hour` (null = no se
  alquila por horas), `stay.rental_mode` (NIGHTLY|HOURLY) + `stay.hours`; el CHECK de fechas
  pasa a `>=` (una estadía por horas entra y sale el mismo día y no bloquea el solape
  nocturno; en HOURLY `stay.rate_per_night` guarda la tarifa pactada POR HORA).
- Reglas: una migración por cambio, nombre descriptivo, **inmutable** tras aplicarse,
  idempotencia cuando aplique (`IF NOT EXISTS` en creación de schemas).
- El modelo de tablas crece **por fases** (`08-development-roadmap.md`): no crear todas las
  tablas finales en Fase 0.

## Auditoría

- `audit` registra eventos sensibles (login, cambios de rol/permiso, operaciones de billing,
  ajustes de inventario). Qué se audita lo define `qa-release-architect` + dominio.

## Catálogos compartidos (ejemplos en `catalog`)

`currencies`, `document_types`, `units`, `business_types`, `verticals`, `modules`,
`person_segments`, `ubigeo`. Son la base para no duplicar configuración por rubro.
