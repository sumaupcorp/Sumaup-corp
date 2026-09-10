-- V7: nucleo ERP (schema erp). Multi-tenant: toda tabla lleva tenant_id.
-- Cadena de valor: producto -> inventario por sucursal -> caja (POS) -> venta + items.
-- PK UUID. Dinero NUMERIC(12,2). Cantidades NUMERIC(14,3).

-- ---------------------------------------------------------------------------
-- Productos
-- ---------------------------------------------------------------------------
CREATE TABLE erp.product (
    id          UUID          PRIMARY KEY,
    tenant_id   UUID          NOT NULL REFERENCES tenant.tenant (id) ON DELETE CASCADE,
    sku         VARCHAR(40)   NOT NULL,
    name        VARCHAR(160)  NOT NULL,
    unit        VARCHAR(20)   NOT NULL DEFAULT 'UNIDAD',
    price       NUMERIC(12,2) NOT NULL DEFAULT 0,
    category    VARCHAR(80),
    is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL,
    UNIQUE (tenant_id, sku)
);
CREATE INDEX idx_product_tenant ON erp.product (tenant_id);

-- ---------------------------------------------------------------------------
-- Clientes del negocio
-- ---------------------------------------------------------------------------
CREATE TABLE erp.customer (
    id          UUID          PRIMARY KEY,
    tenant_id   UUID          NOT NULL REFERENCES tenant.tenant (id) ON DELETE CASCADE,
    doc_type    VARCHAR(20),                       -- DNI, RUC, CE, etc.
    doc_number  VARCHAR(20),
    name        VARCHAR(200)  NOT NULL,
    email       VARCHAR(255),
    phone       VARCHAR(30),
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_customer_tenant ON erp.customer (tenant_id);

-- ---------------------------------------------------------------------------
-- Stock por producto y sucursal
-- ---------------------------------------------------------------------------
CREATE TABLE erp.inventory_stock (
    id          UUID          PRIMARY KEY,
    tenant_id   UUID          NOT NULL REFERENCES tenant.tenant (id) ON DELETE CASCADE,
    product_id  UUID          NOT NULL REFERENCES erp.product (id) ON DELETE CASCADE,
    branch_id   UUID          NOT NULL REFERENCES tenant.branch (id) ON DELETE CASCADE,
    quantity    NUMERIC(14,3) NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL,
    UNIQUE (tenant_id, product_id, branch_id)
);
CREATE INDEX idx_stock_tenant ON erp.inventory_stock (tenant_id);

-- ---------------------------------------------------------------------------
-- Movimientos de inventario (auditoria de stock)
-- ---------------------------------------------------------------------------
CREATE TABLE erp.inventory_movement (
    id          UUID          PRIMARY KEY,
    tenant_id   UUID          NOT NULL REFERENCES tenant.tenant (id) ON DELETE CASCADE,
    product_id  UUID          NOT NULL REFERENCES erp.product (id) ON DELETE CASCADE,
    branch_id   UUID          NOT NULL REFERENCES tenant.branch (id) ON DELETE CASCADE,
    type        VARCHAR(20)   NOT NULL,            -- IN, OUT, ADJUST
    quantity    NUMERIC(14,3) NOT NULL,
    reason      VARCHAR(160),
    ref_id      UUID,                              -- referencia (p. ej. venta)
    created_by  UUID,
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_movement_tenant  ON erp.inventory_movement (tenant_id);
CREATE INDEX idx_movement_product ON erp.inventory_movement (product_id);

-- ---------------------------------------------------------------------------
-- Caja / POS: sesion de caja por sucursal
-- ---------------------------------------------------------------------------
CREATE TABLE erp.cash_session (
    id              UUID          PRIMARY KEY,
    tenant_id       UUID          NOT NULL REFERENCES tenant.tenant (id) ON DELETE CASCADE,
    branch_id       UUID          NOT NULL REFERENCES tenant.branch (id) ON DELETE CASCADE,
    opened_by       UUID,
    opening_amount  NUMERIC(12,2) NOT NULL DEFAULT 0,
    closing_amount  NUMERIC(12,2),
    status          VARCHAR(20)   NOT NULL DEFAULT 'OPEN',  -- OPEN, CLOSED
    opened_at       TIMESTAMPTZ   NOT NULL,
    closed_at       TIMESTAMPTZ,
    created_at      TIMESTAMPTZ   NOT NULL,
    updated_at      TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_cash_session_tenant ON erp.cash_session (tenant_id);
-- Solo una caja abierta por sucursal a la vez.
CREATE UNIQUE INDEX uq_cash_session_open_branch
    ON erp.cash_session (branch_id) WHERE status = 'OPEN';

-- ---------------------------------------------------------------------------
-- Ventas y sus items
-- ---------------------------------------------------------------------------
CREATE TABLE erp.sale (
    id              UUID          PRIMARY KEY,
    tenant_id       UUID          NOT NULL REFERENCES tenant.tenant (id) ON DELETE CASCADE,
    branch_id       UUID          NOT NULL REFERENCES tenant.branch (id) ON DELETE CASCADE,
    cash_session_id UUID          REFERENCES erp.cash_session (id),
    customer_id     UUID          REFERENCES erp.customer (id),
    total           NUMERIC(12,2) NOT NULL DEFAULT 0,
    status          VARCHAR(20)   NOT NULL DEFAULT 'COMPLETED',
    created_by      UUID,
    created_at      TIMESTAMPTZ   NOT NULL,
    updated_at      TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_sale_tenant ON erp.sale (tenant_id);
CREATE INDEX idx_sale_branch ON erp.sale (branch_id);

CREATE TABLE erp.sale_item (
    id          UUID          PRIMARY KEY,
    tenant_id   UUID          NOT NULL REFERENCES tenant.tenant (id) ON DELETE CASCADE,
    sale_id     UUID          NOT NULL REFERENCES erp.sale (id) ON DELETE CASCADE,
    product_id  UUID          NOT NULL REFERENCES erp.product (id),
    quantity    NUMERIC(14,3) NOT NULL,
    unit_price  NUMERIC(12,2) NOT NULL,
    line_total  NUMERIC(12,2) NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_sale_item_sale ON erp.sale_item (sale_id);
