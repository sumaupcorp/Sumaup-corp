# sumaup360-backend

Backend del ecosistema **SUMAUP360**: monolito modular en **Java 21 + Spring Boot**, autoridad
única de identidad efectiva (roles, permisos, tenant, membresía). Firebase prueba *quién eres*;
este backend decide *qué puedes hacer*.

## Stack

- Java 21, Spring Boot 3.4.x, Maven.
- PostgreSQL (una sola base `sumaup360_db`) + Flyway (schemas por dominio).
- Spring Security + JWT + Firebase Admin SDK (verificación de idToken).
- springdoc-openapi (Swagger UI).

## Arquitectura (resumen)

- Multi-tenant por columna `tenant_id` sobre `sumaup360_db`.
- Schemas por dominio: `auth`, `tenant`, `app`, `erp`, `billing`, `audit`, `chatbot`,
  `catalog`, `notification`.
- Dos líneas que **no se mezclan**: Personas (`app`) y Negocios (`erp`). Lo transversal
  (`auth`, `users`, `roles`, `permissions`, `billing`, `notification`, `audit`, `chatbot`,
  `catalog`) se comparte.
- Rubros por configuración (catálogos + módulos habilitados), no por código duplicado.

Detalle en `../docs/02-backend-architecture.md`, `../docs/03-database-design.md`,
`../docs/06-auth-security.md`.

## Estado

Fase **1 — Identidad + RBAC + multi-tenant** (funcional, verificado contra PostgreSQL 18).
Incluye: bootstrap del proyecto, capa común (errores, `BaseEntity`), seguridad (verificación
Firebase + modo dev local), provisión de identidad, RBAC con `@PreAuthorize`, núcleo
multi-tenant (tenant/empresa/sucursal) y endpoint `/api/v1/me`. Migraciones `V1`–`V5`.

Lo construido y lo que sigue se detalla en `../docs/08-development-roadmap.md`.

## Cómo correr (local)

Requisitos: JDK 21 y PostgreSQL con la base `sumaup360_db` (host `localhost:5432`,
user/pass `postgres/postgres`). **No necesitas instalar Maven**: usa el wrapper incluido
(`mvnw`/`mvnw.cmd`).

```bash
# Desde sumaup360-backend/
./mvnw spring-boot:run          # Windows: .\mvnw.cmd spring-boot:run
# Compilar / pruebas
./mvnw clean verify
```

Flyway aplica las migraciones de `src/main/resources/db/migration` al arrancar.

- Swagger UI: http://localhost:8080/swagger-ui.html
- Health (público): http://localhost:8080/api/v1/health

### Modo desarrollo (sin Firebase todavía)

`application.yml` trae `security.dev-mode: true`. Si Firebase aún no está configurado, puedes
autenticarte con el header `X-Debug-Uid`:

```bash
# Usuario admin (bootstrap): provisiona STAFF admin con todos los permisos
curl -H "X-Debug-Uid: dev-admin" http://localhost:8080/api/v1/me

# Usuario normal: se provisiona como PERSON sin roles (las acciones protegidas dan 403)
curl -H "X-Debug-Uid: juan123"  http://localhost:8080/api/v1/me
```

El uid del bootstrap admin se controla con `security.bootstrap-admin-uid` (por defecto
`dev-admin`).

### Producción / con Firebase real

Define el service account y desactiva el modo dev:

```bash
export FIREBASE_SERVICE_ACCOUNT=/ruta/firebase-service-account.json
export SECURITY_DEV_MODE=false
```

Con Firebase activo, el cliente envía `Authorization: Bearer <idToken>` y el backend lo
verifica con el Admin SDK. **Nunca** dejes `dev-mode=true` ni el header `X-Debug-Uid` en
producción.

> Maven se descargó en `~/.sumaup-tools/apache-maven-3.9.9` para generar el wrapper; con el
> wrapper ya no hace falta. Puedes borrar esa carpeta si quieres.
