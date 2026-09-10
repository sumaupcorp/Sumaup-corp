-- V58: campanas de push programables desde el Backoffice y configuracion de
-- notificaciones automaticas (eventos y recordatorios) de la Linea Personas.

-- Campanas de push (publicidad/recordatorios) creadas por el staff.
CREATE TABLE notification.campaign (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    title         VARCHAR(120)  NOT NULL,
    body          VARCHAR(500)  NOT NULL,
    route         VARCHAR(120),                                   -- ruta de la app al tocar el push
    audience      VARCHAR(30)   NOT NULL,                         -- ALL | TAXISTA | DELIVERY_PEYA | ...
    scheduled_at  TIMESTAMPTZ,                                    -- NULL = envio inmediato
    status        VARCHAR(15)   NOT NULL DEFAULT 'SCHEDULED',     -- SCHEDULED | SENDING | SENT | CANCELLED
    sent_count    INT           NOT NULL DEFAULT 0,
    failed_count  INT           NOT NULL DEFAULT 0,
    created_by    UUID,                                           -- staff que la creo
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    sent_at       TIMESTAMPTZ
);

CREATE INDEX idx_campaign_status_scheduled ON notification.campaign (status, scheduled_at);

-- Configuracion de notificaciones automaticas: eventos (RECEIPT_*) y recordatorios (REMINDER_*).
CREATE TABLE notification.notification_setting (
    key         VARCHAR(40)   PRIMARY KEY,
    enabled     BOOLEAN       NOT NULL,
    title       VARCHAR(120)  NOT NULL,
    body        VARCHAR(500)  NOT NULL,
    description VARCHAR(200)  NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

INSERT INTO notification.notification_setting (key, enabled, title, body, description) VALUES
    ('RECEIPT_PROCESSED', true, 'Comprobante listo',
     'Tu comprobante fue procesado y ya esta disponible en la app.',
     'Se envia al usuario cuando el staff procesa su comprobante.'),
    ('RECEIPT_OBSERVED', true, 'Comprobante observado',
     'Tu comprobante necesita una correccion. Revisalo en la app.',
     'Se envia cuando el staff observa un comprobante.'),
    ('REMINDER_ONBOARDING', false, 'Completa tu registro',
     'Te falta poco para terminar de configurar tu cuenta. Continua donde te quedaste.',
     'Recordatorio diario a usuarios que no completaron el onboarding.'),
    ('REMINDER_ORIENTATION', false, 'Tu orientacion tributaria te espera',
     'Responde 5 preguntas y descubre que opcion tributaria te conviene.',
     'Recordatorio diario a usuarios con la orientacion pendiente.'),
    ('REMINDER_RECEIPTS_OBSERVED', false, 'Tienes comprobantes por corregir',
     'Uno o mas comprobantes fueron observados. Revisalos en la app.',
     'Recordatorio diario a usuarios con comprobantes observados.');
