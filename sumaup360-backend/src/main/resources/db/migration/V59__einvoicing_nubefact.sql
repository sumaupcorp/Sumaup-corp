-- V59: credenciales de FACTURACION ELECTRONICA por empresa (proveedor NubeFact) + permisos.
-- NubeFact asigna a cada RUC emisor una RUTA (URL unica) y un TOKEN. El token se guarda
-- CIFRADO (CryptoService, AES-256-GCM). Un fallback demo global vive en application.yml
-- (nubefact.demo-ruta / demo-token) para pruebas contra el entorno de demostracion.

CREATE TABLE erp.company_einvoicing_config (
    id                  UUID PRIMARY KEY,
    tenant_id           UUID NOT NULL,
    company_id          UUID NOT NULL,
    provider            VARCHAR(20)  NOT NULL DEFAULT 'NUBEFACT',
    nubefact_ruta       VARCHAR(500),
    nubefact_token_enc  VARCHAR(1000),
    enabled             BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_einvoicing_config_company UNIQUE (tenant_id, company_id)
);
CREATE INDEX idx_einvoicing_config_company ON erp.company_einvoicing_config (tenant_id, company_id);

-- Permisos: configurar credenciales (sensible) y emitir comprobante electronico.
INSERT INTO auth.permission (id, code, description, created_at, updated_at)
SELECT gen_random_uuid(), c.code, c.description, now(), now()
FROM (VALUES
    ('einvoice:config', 'Configurar facturacion electronica (credenciales NubeFact)'),
    ('einvoice:emit',   'Emitir comprobante electronico ante SUNAT')
) AS c(code, description)
ON CONFLICT (code) DO NOTHING;

-- Staff global admin recibe ambos.
INSERT INTO auth.role_permission (role_id, permission_id)
SELECT ro.id, pe.id
FROM auth.role ro
JOIN auth.permission pe ON pe.code IN ('einvoice:config', 'einvoice:emit')
WHERE ro.code = 'admin' AND ro.tenant_id IS NULL
ON CONFLICT DO NOTHING;

-- einvoice:emit -> a todo rol que ya pueda registrar ventas (sale:create).
INSERT INTO auth.role_permission (role_id, permission_id)
SELECT DISTINCT rp.role_id, pe.id
FROM auth.role_permission rp
JOIN auth.permission src ON src.id = rp.permission_id AND src.code = 'sale:create'
JOIN auth.permission pe  ON pe.code = 'einvoice:emit'
ON CONFLICT DO NOTHING;

-- einvoice:config -> a todo rol que ya gestione series de documentos (document-series:manage).
INSERT INTO auth.role_permission (role_id, permission_id)
SELECT DISTINCT rp.role_id, pe.id
FROM auth.role_permission rp
JOIN auth.permission src ON src.id = rp.permission_id AND src.code = 'document-series:manage'
JOIN auth.permission pe  ON pe.code = 'einvoice:config'
ON CONFLICT DO NOTHING;
