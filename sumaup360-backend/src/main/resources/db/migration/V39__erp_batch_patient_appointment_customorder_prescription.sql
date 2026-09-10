-- V39: dominio ERP reutilizable para los rubros (schema erp). Multi-tenant: tenant_id en todo.
-- Lotes/vencimientos (pharmacy, bakery, minimarket), pacientes/mascotas y citas (veterinary,
-- beauty), pedidos por encargo (bakery, pastry) y recetas MVP (pharmacy).
-- Nada es "de un rubro": son modulos habilitables (catalog.business_type_module).

-- ---------------------------------------------------------------------------
-- Lotes y vencimientos por producto y sucursal
-- ---------------------------------------------------------------------------
CREATE TABLE erp.product_batch (
    id          UUID          PRIMARY KEY,
    tenant_id   UUID          NOT NULL REFERENCES tenant.tenant (id) ON DELETE CASCADE,
    branch_id   UUID          NOT NULL REFERENCES tenant.branch (id) ON DELETE CASCADE,
    product_id  UUID          NOT NULL REFERENCES erp.product (id) ON DELETE CASCADE,
    batch_code  VARCHAR(60)   NOT NULL,
    expiry_date DATE,
    quantity    NUMERIC(14,3) NOT NULL DEFAULT 0,
    status      VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',   -- ACTIVE, DEPLETED, EXPIRED, RECALLED
    notes       VARCHAR(300),
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL,
    UNIQUE (tenant_id, branch_id, product_id, batch_code)
);
CREATE INDEX idx_batch_tenant ON erp.product_batch (tenant_id, branch_id);
CREATE INDEX idx_batch_expiry ON erp.product_batch (tenant_id, expiry_date);

-- ---------------------------------------------------------------------------
-- Pacientes / mascotas (dueno = cliente del negocio)
-- ---------------------------------------------------------------------------
CREATE TABLE erp.patient (
    id          UUID          PRIMARY KEY,
    tenant_id   UUID          NOT NULL REFERENCES tenant.tenant (id) ON DELETE CASCADE,
    customer_id UUID          NOT NULL REFERENCES erp.customer (id) ON DELETE CASCADE,
    name        VARCHAR(80)   NOT NULL,
    species     VARCHAR(40),                       -- perro, gato, ave...
    breed       VARCHAR(60),
    sex         VARCHAR(10),                       -- MACHO, HEMBRA
    birth_date  DATE,
    weight_kg   NUMERIC(6,2),
    notes       VARCHAR(500),
    is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_patient_tenant   ON erp.patient (tenant_id);
CREATE INDEX idx_patient_customer ON erp.patient (tenant_id, customer_id);

-- ---------------------------------------------------------------------------
-- Citas (veterinaria, salon de belleza; generico por sucursal)
-- ---------------------------------------------------------------------------
CREATE TABLE erp.appointment (
    id               UUID          PRIMARY KEY,
    tenant_id        UUID          NOT NULL REFERENCES tenant.tenant (id) ON DELETE CASCADE,
    branch_id        UUID          NOT NULL REFERENCES tenant.branch (id) ON DELETE CASCADE,
    customer_id      UUID          NOT NULL REFERENCES erp.customer (id),
    patient_id       UUID          REFERENCES erp.patient (id),   -- null si el rubro no maneja pacientes
    reason           VARCHAR(160),
    scheduled_at     TIMESTAMPTZ   NOT NULL,
    duration_minutes INT           NOT NULL DEFAULT 30,
    status           VARCHAR(20)   NOT NULL DEFAULT 'SCHEDULED',  -- SCHEDULED, CONFIRMED, COMPLETED, CANCELED, NO_SHOW
    assigned_to      UUID,                                        -- usuario que atiende (sin FK, como sale.created_by)
    notes            VARCHAR(500),
    created_at       TIMESTAMPTZ   NOT NULL,
    updated_at       TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_appointment_agenda ON erp.appointment (tenant_id, branch_id, scheduled_at);

-- ---------------------------------------------------------------------------
-- Pedidos por encargo (panaderia, pasteleria)
-- ---------------------------------------------------------------------------
CREATE TABLE erp.custom_order (
    id             UUID          PRIMARY KEY,
    tenant_id      UUID          NOT NULL REFERENCES tenant.tenant (id) ON DELETE CASCADE,
    branch_id      UUID          NOT NULL REFERENCES tenant.branch (id) ON DELETE CASCADE,
    customer_id    UUID          NOT NULL REFERENCES erp.customer (id),
    description    VARCHAR(500)  NOT NULL,
    delivery_at    TIMESTAMPTZ   NOT NULL,
    total_amount   NUMERIC(12,2) NOT NULL DEFAULT 0,
    advance_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    status         VARCHAR(20)   NOT NULL DEFAULT 'PENDING',   -- PENDING, IN_PROGRESS, READY, DELIVERED, CANCELED
    sale_id        UUID          REFERENCES erp.sale (id),     -- venta generada al cobrar
    notes          VARCHAR(300),
    created_at     TIMESTAMPTZ   NOT NULL,
    updated_at     TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_corder_delivery ON erp.custom_order (tenant_id, branch_id, delivery_at);
CREATE INDEX idx_corder_status   ON erp.custom_order (tenant_id, status);

-- ---------------------------------------------------------------------------
-- Recetas (farmacia, MVP): registro simple, sin flujo de dispensacion
-- ---------------------------------------------------------------------------
CREATE TABLE erp.prescription (
    id             UUID          PRIMARY KEY,
    tenant_id      UUID          NOT NULL REFERENCES tenant.tenant (id) ON DELETE CASCADE,
    branch_id      UUID          NOT NULL REFERENCES tenant.branch (id) ON DELETE CASCADE,
    customer_id    UUID          REFERENCES erp.customer (id),
    sale_id        UUID          REFERENCES erp.sale (id),
    doctor_name    VARCHAR(120)  NOT NULL,
    doctor_license VARCHAR(20),                     -- CMP
    issued_date    DATE          NOT NULL,
    diagnosis      VARCHAR(200),
    medications    TEXT          NOT NULL,          -- medicamentos, dosis e indicaciones
    notes          VARCHAR(300),
    created_at     TIMESTAMPTZ   NOT NULL,
    updated_at     TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_prescription_tenant ON erp.prescription (tenant_id, issued_date);

-- ---------------------------------------------------------------------------
-- Permisos de los modulos nuevos
-- ---------------------------------------------------------------------------
INSERT INTO auth.permission (id, code, description, created_at, updated_at)
SELECT gen_random_uuid(), c.code, c.description, now(), now()
FROM (VALUES
    ('batch:read',           'Ver lotes y vencimientos'),
    ('batch:manage',         'Gestionar lotes y vencimientos'),
    ('patient:read',         'Ver pacientes / mascotas'),
    ('patient:manage',       'Gestionar pacientes / mascotas'),
    ('appointment:read',     'Ver citas'),
    ('appointment:manage',   'Gestionar citas'),
    ('custom-order:read',    'Ver pedidos por encargo'),
    ('custom-order:manage',  'Gestionar pedidos por encargo'),
    ('prescription:read',    'Ver recetas'),
    ('prescription:manage',  'Gestionar recetas')
) AS c(code, description)
ON CONFLICT (code) DO NOTHING;

-- Admin global (staff) recibe todo.
INSERT INTO auth.role_permission (role_id, permission_id)
SELECT ro.id, pe.id
FROM auth.role ro
JOIN auth.permission pe ON pe.code IN
    ('batch:read','batch:manage','patient:read','patient:manage',
     'appointment:read','appointment:manage','custom-order:read','custom-order:manage',
     'prescription:read','prescription:manage')
WHERE ro.code = 'admin' AND ro.tenant_id IS NULL
ON CONFLICT DO NOTHING;

-- Los tenant-admin EXISTENTES tambien (los nuevos los reciben via RoleService).
INSERT INTO auth.role_permission (role_id, permission_id)
SELECT ro.id, pe.id
FROM auth.role ro
JOIN auth.permission pe ON pe.code IN
    ('batch:read','batch:manage','patient:read','patient:manage',
     'appointment:read','appointment:manage','custom-order:read','custom-order:manage',
     'prescription:read','prescription:manage')
WHERE ro.code = 'tenant-admin' AND ro.tenant_id IS NOT NULL
ON CONFLICT DO NOTHING;
