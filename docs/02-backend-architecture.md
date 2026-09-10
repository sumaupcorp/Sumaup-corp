# 02 — Arquitectura del backend

Proyecto: `sumaup360-backend`. Java 21 · Spring Boot · Maven · PostgreSQL · Flyway ·
Spring Security · JWT · Firebase Admin SDK · Swagger/OpenAPI. **Monolito modular**.

## Estilo arquitectónico

- **Monolito modular:** un despliegue, módulos internos con bajo acoplamiento, listos para
  extraerse a servicios en el futuro (no ahora).
- Cada módulo expone su API y oculta su implementación. La comunicación entre módulos se hace
  por interfaces de servicio, no tocando tablas ajenas.
- Por módulo (capas): `web` (controllers/DTOs) → `service` (reglas) → `domain` (entidades) →
  `repository` (acceso a datos). `config` y `common` transversales.

## Mapa de módulos (paquetes `com.sumaup360.*`)

Transversales / plataforma:
- `common` — utilidades, errores, paginación, respuestas.
- `config` — configuración Spring, OpenAPI, CORS, beans.
- `security` — Spring Security, filtro de verificación Firebase, method security.
- `auth` — identidad interna, vínculo `firebase_uid`, claims.
- `tenant` — tenants, contexto de tenant, resolución por request.
- `users`, `roles`, `permissions` — RBAC.
- `catalog` — catálogos compartidos.
- `billing`, `subscription` — planes, membresías, pagos.
- `audit` — auditoría.
- `chatbot` — Suma.
- `notification` — notificaciones.

Línea Personas (`app`):
- `app/diagnosis`, `app/income`, `app/expense`, `app/receipt`, `app/alert`.

Línea Negocios (`erp`):
- `erp/company`, `erp/branch`, `erp/business-type`, `erp/module`, `erp/customer`,
  `erp/product`, `erp/inventory`, `erp/sale`, `erp/pos`, `erp/invoice`, `erp/supplier`,
  `erp/report`.

## Seguridad (resumen, ver 06)

- Filtro que extrae `Authorization: Bearer <idToken>`, lo verifica con Firebase Admin,
  resuelve la identidad interna y arma el `SecurityContext` con roles/permisos y tenant.
- Autorización a nivel de método (`@PreAuthorize`) por permiso, más filtro de `tenant_id`.

## Persistencia y migraciones

- Spring Data JPA sobre PostgreSQL. Una sola base `sumaup360_db`, schemas por dominio.
- **Flyway** crea schemas y tablas. Migración inmutable: cambios = nueva `V{n}`.
- `V1__create_schemas.sql` crea los 9 schemas; `V2__initial_catalogs.sql` siembra catálogos
  mínimos. El resto del modelo crece por fases.

## Configuración

- `application.yml` con perfil `local`: PostgreSQL `localhost:5432`, db `sumaup360_db`,
  user/pass `postgres/postgres`, Flyway habilitado, OpenAPI habilitado.
- Credenciales de Firebase Admin por archivo de service account fuera del control de
  versiones (variable de entorno / ruta configurable).

## Documentación de API

- springdoc-openapi expone Swagger UI. Cada controller documenta sus endpoints, códigos y
  DTOs. Los contratos publicados aquí son los que consumen SaaS y mobile.

## Reglas

- No implementar lógica completa en Fase 0: solo estructura, config y migraciones base.
- Personas y Negocios no comparten servicios de dominio (solo lo transversal).
- Todo acceso multi-tenant filtra por `tenant_id`.
