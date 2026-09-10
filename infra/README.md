# infra — Infraestructura del ecosistema SUMAUP360

Infraestructura local y notas de despliegue. En Fase 0 solo PostgreSQL local; el resto se
documenta para fases posteriores.

## Contenido

- `docker/docker-compose.local.yml` — PostgreSQL local para desarrollo (opcional si ya tienes
  PostgreSQL instalado en tu máquina).
- `docker/postgres/init.sql` — crea la base `sumaup360_db` al levantar el contenedor. Los
  **schemas y tablas NO se crean aquí**: los crea el backend con Flyway.
- `deployment/backend.md`, `deployment/frontend.md`, `deployment/mobile.md` — notas de
  despliegue por proyecto (pendientes de Fase 9).

## PostgreSQL local

Tienes dos opciones; elige una.

### Opción A — PostgreSQL ya instalado (recomendado si lo tienes)
Crea la base manualmente una vez:
```
psql -U postgres -h localhost -c "CREATE DATABASE sumaup360_db;"
```
El backend (perfil `local`) se conecta a `localhost:5432`, db `sumaup360_db`,
user/pass `postgres/postgres`.

### Opción B — Docker
```
cd infra/docker
docker compose -f docker-compose.local.yml up -d
```
Esto levanta PostgreSQL 16 en `localhost:5432` y crea `sumaup360_db` con `init.sql`.

En ambos casos, al arrancar el backend, Flyway aplica `V1` (schemas) y `V2` (catálogos).
