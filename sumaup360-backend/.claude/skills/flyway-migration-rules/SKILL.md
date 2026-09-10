---
name: flyway-migration-rules
description: >-
  Reglas de migraciones Flyway en SUMAUP360: inmutabilidad, nomenclatura V{n}__, schemas por
  dominio, snake_case, índices e idempotencia. Úsalo siempre que crees o modifiques la base de
  datos del backend.
---

# flyway-migration-rules

Las migraciones viven en `src/main/resources/db/migration` y se aplican al arrancar.

## Reglas duras

- **Inmutabilidad**: una migración ya aplicada NUNCA se edita ni se borra. Cambios = nueva `V{n+1}`.
- **Nombre**: `V{n}__descripcion_en_snake_case.sql`. `n` incremental y único.
- **Schemas**: solo se crean/usan los schemas del dominio: `auth`, `tenant`, `app`, `erp`,
  `billing`, `audit`, `chatbot`, `catalog`, `notification`. Tablas siempre calificadas con su schema.
- **Estilo**: `snake_case`; PK `BIGINT GENERATED ALWAYS AS IDENTITY`; `IF NOT EXISTS` en objetos.
- **Seeds idempotentes**: usar `ON CONFLICT (...) DO NOTHING` para datos semilla.
- **Multi-tenant**: tablas de dominio de tenant llevan `tenant_id` con índice.

## Índices

- Indexar `tenant_id` y las columnas usadas en filtros frecuentes y FKs.
- Unicidad de códigos de catálogo vía `UNIQUE`.

## Coherencia con JPA

- `ddl-auto: validate`: el esquema creado por Flyway debe coincidir exactamente con las entidades.
- No usar Hibernate para crear/alterar tablas; la estructura es solo de Flyway.

Ver migraciones base `V1__create_schemas.sql` y `V2__initial_catalogs.sql`.
