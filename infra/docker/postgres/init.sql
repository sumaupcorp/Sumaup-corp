-- Inicialización de PostgreSQL para SUMAUP360 (entorno local con Docker).
-- Solo crea la base. Los SCHEMAS y TABLAS los crea el backend con Flyway (V1, V2, ...).
-- Si POSTGRES_DB ya creó sumaup360_db, este bloque es redundante pero seguro.

SELECT 'CREATE DATABASE sumaup360_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'sumaup360_db')\gexec
