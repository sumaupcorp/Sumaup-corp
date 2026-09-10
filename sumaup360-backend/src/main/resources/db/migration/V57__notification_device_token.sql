-- V57: tokens de dispositivo (FCM) para notificaciones push de la app movil (Linea Personas).
-- Un usuario puede tener varios dispositivos; el token es unico globalmente y se reasigna
-- al ultimo usuario que inicio sesion en ese dispositivo.

CREATE SCHEMA IF NOT EXISTS notification;

CREATE TABLE notification.device_token (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES auth.app_user (id) ON DELETE CASCADE,
    token       TEXT         NOT NULL UNIQUE,
    platform    VARCHAR(10)  NOT NULL,          -- android | ios | web
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_device_token_user ON notification.device_token (user_id);
