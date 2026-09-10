-- V1: creacion de schemas por dominio de SUMAUP360.
-- Una sola base (sumaup360_db) con separacion logica por dominio.
-- Lo transversal (auth, tenant, billing, audit, chatbot, catalog, notification) se comparte;
-- las dos lineas de producto viven separadas: Personas (app) y Negocios (erp).

CREATE SCHEMA IF NOT EXISTS auth;          -- identidad efectiva: usuarios, claims, sesiones
CREATE SCHEMA IF NOT EXISTS tenant;        -- tenants y membresias
CREATE SCHEMA IF NOT EXISTS app;           -- Linea Personas (app movil)
CREATE SCHEMA IF NOT EXISTS erp;           -- Linea Negocios (SaaS / ERP)
CREATE SCHEMA IF NOT EXISTS billing;       -- planes, suscripciones, pagos
CREATE SCHEMA IF NOT EXISTS audit;         -- auditoria transversal
CREATE SCHEMA IF NOT EXISTS chatbot;       -- chatbot Suma
CREATE SCHEMA IF NOT EXISTS catalog;       -- catalogos compartidos
CREATE SCHEMA IF NOT EXISTS notification;  -- notificaciones
