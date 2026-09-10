-- V11: vertical Restaurantes (schema erp). Extiende el ERP core: mesas, comandas y cocina.
-- Es un MODULO habilitable (no se duplica por rubro). Multi-tenant: todo lleva tenant_id.
-- Flujo: mesa -> comanda (kitchen_order) + items -> cocina (estados) -> cobro (crea venta).

-- ---------------------------------------------------------------------------
-- Mesas
-- ---------------------------------------------------------------------------
CREATE TABLE erp.restaurant_table (
    id          UUID          PRIMARY KEY,
    tenant_id   UUID          NOT NULL REFERENCES tenant.tenant (id) ON DELETE CASCADE,
    branch_id   UUID          NOT NULL REFERENCES tenant.branch (id) ON DELETE CASCADE,
    name        VARCHAR(60)   NOT NULL,
    zone        VARCHAR(80),                       -- salon / ambiente
    capacity    INT           NOT NULL DEFAULT 4,
    status      VARCHAR(20)   NOT NULL DEFAULT 'FREE',   -- FREE, OCCUPIED, RESERVED
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL,
    UNIQUE (tenant_id, branch_id, name)
);
CREATE INDEX idx_rtable_branch ON erp.restaurant_table (tenant_id, branch_id);

-- ---------------------------------------------------------------------------
-- Comandas (kitchen_order)
-- ---------------------------------------------------------------------------
CREATE TABLE erp.kitchen_order (
    id            UUID          PRIMARY KEY,
    tenant_id     UUID          NOT NULL REFERENCES tenant.tenant (id) ON DELETE CASCADE,
    branch_id     UUID          NOT NULL REFERENCES tenant.branch (id) ON DELETE CASCADE,
    table_id      UUID          REFERENCES erp.restaurant_table (id),
    order_number  VARCHAR(30)   NOT NULL,
    type          VARCHAR(20)   NOT NULL DEFAULT 'DINE_IN',  -- DINE_IN, TAKEAWAY, DELIVERY
    status        VARCHAR(20)   NOT NULL DEFAULT 'OPEN',     -- OPEN, IN_KITCHEN, READY, SERVED, BILLED, CANCELLED
    notes         VARCHAR(300),
    total         NUMERIC(12,2) NOT NULL DEFAULT 0,
    sale_id       UUID          REFERENCES erp.sale (id),
    created_by    UUID,
    created_at    TIMESTAMPTZ   NOT NULL,
    updated_at    TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_korder_branch ON erp.kitchen_order (tenant_id, branch_id);
CREATE INDEX idx_korder_status ON erp.kitchen_order (branch_id, status);

-- ---------------------------------------------------------------------------
-- Items de comanda
-- ---------------------------------------------------------------------------
CREATE TABLE erp.kitchen_order_item (
    id                UUID          PRIMARY KEY,
    tenant_id         UUID          NOT NULL REFERENCES tenant.tenant (id) ON DELETE CASCADE,
    kitchen_order_id  UUID          NOT NULL REFERENCES erp.kitchen_order (id) ON DELETE CASCADE,
    product_id        UUID          NOT NULL REFERENCES erp.product (id),
    quantity          NUMERIC(14,3) NOT NULL,
    unit_price        NUMERIC(12,2) NOT NULL,
    line_total        NUMERIC(12,2) NOT NULL,
    notes             VARCHAR(200),
    station           VARCHAR(60),                 -- area de cocina (parrilla, frio, barra...)
    status            VARCHAR(20)   NOT NULL DEFAULT 'PENDING',  -- PENDING, PREPARING, READY, SERVED
    created_at        TIMESTAMPTZ   NOT NULL,
    updated_at        TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_koitem_order ON erp.kitchen_order_item (kitchen_order_id);

-- ---------------------------------------------------------------------------
-- Permisos del vertical
-- ---------------------------------------------------------------------------
INSERT INTO auth.permission (id, code, description, created_at, updated_at)
SELECT gen_random_uuid(), c.code, c.description, now(), now()
FROM (VALUES
    ('table:read',     'Ver mesas'),
    ('table:manage',   'Gestionar mesas'),
    ('order:read',     'Ver comandas'),
    ('order:create',   'Crear comandas y agregar items'),
    ('order:manage',   'Gestionar comandas (cancelar, cobrar)'),
    ('kitchen:read',   'Ver cola de cocina'),
    ('kitchen:operate','Operar cocina (cambiar estados de items)')
) AS c(code, description)
ON CONFLICT (code) DO NOTHING;

INSERT INTO auth.role_permission (role_id, permission_id)
SELECT ro.id, pe.id
FROM auth.role ro
JOIN auth.permission pe ON pe.code IN
    ('table:read','table:manage','order:read','order:create','order:manage',
     'kitchen:read','kitchen:operate')
WHERE ro.code = 'admin' AND ro.tenant_id IS NULL
ON CONFLICT DO NOTHING;
