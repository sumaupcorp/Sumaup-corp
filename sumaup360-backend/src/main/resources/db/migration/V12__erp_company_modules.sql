-- V12: motor de modulos habilitados por empresa (Fase 6).
-- defaults(rubro) +/- overrides(empresa) = enabledModules. El techo por plan se aplicara
-- cuando exista billing (TODO Fase 8). No se duplica codigo por rubro: todo es configuracion.

-- ---------------------------------------------------------------------------
-- Modulos adicionales (verticales / por rubro) en el catalogo
-- ---------------------------------------------------------------------------
INSERT INTO catalog.modules (code, name, is_core) VALUES
    ('tables',         'Mesas',                 FALSE),
    ('kitchen',        'Cocina / Comandas',     FALSE),
    ('menu',           'Carta',                 FALSE),
    ('batch-expiry',   'Lotes y vencimientos',  FALSE),
    ('prescription',   'Recetas',               FALSE),
    ('variants',       'Variantes y unidades',  FALSE),
    ('appointments',   'Citas',                 FALSE),
    ('service-orders', 'Ordenes de servicio',   FALSE)
ON CONFLICT (code) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Modulos por rubro por defecto (catalogo global de configuracion)
-- ---------------------------------------------------------------------------
CREATE TABLE catalog.business_type_module (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    business_type_code  VARCHAR(40) NOT NULL,
    module_code         VARCHAR(40) NOT NULL,
    UNIQUE (business_type_code, module_code)
);

-- Todos los rubros reciben los modulos core.
INSERT INTO catalog.business_type_module (business_type_code, module_code)
SELECT bt.code, m.code
FROM catalog.business_types bt
CROSS JOIN catalog.modules m
WHERE m.is_core = TRUE
ON CONFLICT DO NOTHING;

-- Extras por rubro (no se duplica codigo: son modulos que se habilitan).
INSERT INTO catalog.business_type_module (business_type_code, module_code) VALUES
    ('restaurant', 'tables'),
    ('restaurant', 'kitchen'),
    ('restaurant', 'menu'),
    ('pharmacy',   'batch-expiry'),
    ('pharmacy',   'prescription'),
    ('hardware',   'variants'),
    ('beauty',     'appointments'),
    ('laundry',    'service-orders')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- Modulos habilitados por empresa (estado efectivo, con override)
-- ---------------------------------------------------------------------------
CREATE TABLE erp.company_module (
    id          UUID         PRIMARY KEY,
    tenant_id   UUID         NOT NULL REFERENCES tenant.tenant (id) ON DELETE CASCADE,
    company_id  UUID         NOT NULL REFERENCES tenant.company (id) ON DELETE CASCADE,
    module_code VARCHAR(40)  NOT NULL,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    source      VARCHAR(20)  NOT NULL DEFAULT 'DEFAULT',   -- DEFAULT (del rubro) | OVERRIDE (admin)
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    UNIQUE (tenant_id, company_id, module_code)
);
CREATE INDEX idx_company_module ON erp.company_module (tenant_id, company_id);
