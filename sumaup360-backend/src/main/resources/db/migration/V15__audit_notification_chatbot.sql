-- V15: modulos transversales de cierre (Fase 9): auditoria, notificaciones y chatbot Suma.

-- ---------------------------------------------------------------------------
-- Auditoria transversal (append-only). La escribe un interceptor en cada mutacion.
-- ---------------------------------------------------------------------------
CREATE TABLE audit.audit_event (
    id              UUID         PRIMARY KEY,
    occurred_at     TIMESTAMPTZ  NOT NULL,
    actor_user_id   UUID,
    actor_type      VARCHAR(20),               -- PERSON, BUSINESS, STAFF, ANONYMOUS
    method          VARCHAR(10),
    path            VARCHAR(300),
    action          VARCHAR(160),
    status          INT,
    tenant_id       UUID,
    ip              VARCHAR(60)
);
CREATE INDEX idx_audit_occurred ON audit.audit_event (occurred_at DESC);
CREATE INDEX idx_audit_actor ON audit.audit_event (actor_user_id);

-- ---------------------------------------------------------------------------
-- Notificaciones in-app (por usuario destinatario)
-- ---------------------------------------------------------------------------
CREATE TABLE notification.notification (
    id                  UUID         PRIMARY KEY,
    recipient_user_id   UUID         NOT NULL REFERENCES auth.app_user (id) ON DELETE CASCADE,
    title               VARCHAR(160) NOT NULL,
    body                VARCHAR(1000),
    type                VARCHAR(30)  NOT NULL DEFAULT 'INFO',
    is_read             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_notification_recipient ON notification.notification (recipient_user_id, is_read);

-- ---------------------------------------------------------------------------
-- Chatbot Suma: conversaciones y mensajes (por usuario)
-- ---------------------------------------------------------------------------
CREATE TABLE chatbot.conversation (
    id          UUID         PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES auth.app_user (id) ON DELETE CASCADE,
    title       VARCHAR(160),
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_conversation_user ON chatbot.conversation (user_id);

CREATE TABLE chatbot.message (
    id               UUID          PRIMARY KEY,
    conversation_id  UUID          NOT NULL REFERENCES chatbot.conversation (id) ON DELETE CASCADE,
    role             VARCHAR(20)   NOT NULL,        -- USER, ASSISTANT
    content          VARCHAR(2000) NOT NULL,
    created_at       TIMESTAMPTZ   NOT NULL,
    updated_at       TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_message_conversation ON chatbot.message (conversation_id);

-- ---------------------------------------------------------------------------
-- Permiso para enviar notificaciones (staff)
-- ---------------------------------------------------------------------------
INSERT INTO auth.permission (id, code, description, created_at, updated_at)
SELECT gen_random_uuid(), 'notification:send', 'Enviar notificaciones', now(), now()
ON CONFLICT (code) DO NOTHING;

INSERT INTO auth.role_permission (role_id, permission_id)
SELECT ro.id, pe.id
FROM auth.role ro
JOIN auth.permission pe ON pe.code = 'notification:send'
WHERE ro.code = 'admin' AND ro.tenant_id IS NULL
ON CONFLICT DO NOTHING;
