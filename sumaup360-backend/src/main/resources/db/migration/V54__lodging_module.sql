-- V54: rubro hospedaje (hoteles, hostales, apart-hoteles, albergues) y modulo lodging.
-- TDR: docs/11-rubro-hospedaje.md. El hospedaje NO reutiliza citas: modela ocupacion
-- exclusiva de una habitacion por rango de noches (stay), con registro legal de huespedes
-- y cargos a la habitacion que se liquidan como venta del POS en el check-out.

-- ---------------------------------------------------------------------------
-- Rubro y sub-rubros (clases del Reglamento de Establecimientos de Hospedaje)
-- ---------------------------------------------------------------------------
INSERT INTO catalog.business_types (code, name) VALUES
    ('lodging', 'Hotel / Hospedaje')
ON CONFLICT (code) DO NOTHING;

INSERT INTO catalog.verticals (business_type_id, code, name)
SELECT bt.id, v.code, v.name
FROM (VALUES
    ('lodging', 'hotel',       'Hotel'),
    ('lodging', 'hostal',      'Hostal'),
    ('lodging', 'apart-hotel', 'Apart-hotel'),
    ('lodging', 'albergue',    'Albergue'),
    ('lodging', 'hospedaje',   'Hospedaje / Casa de huespedes')
) AS v(business_type_code, code, name)
JOIN catalog.business_types bt ON bt.code = v.business_type_code
ON CONFLICT (code) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Modulo lodging + defaults del rubro (core V12 solo corrio para rubros de entonces)
-- ---------------------------------------------------------------------------
INSERT INTO catalog.modules (code, name, is_core) VALUES
    ('lodging', 'Habitaciones y hospedaje', FALSE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO catalog.business_type_module (business_type_code, module_code)
SELECT bt.code, m.code
FROM catalog.business_types bt
CROSS JOIN catalog.modules m
WHERE m.is_core = TRUE
  AND bt.code = 'lodging'
ON CONFLICT DO NOTHING;

INSERT INTO catalog.business_type_module (business_type_code, module_code) VALUES
    ('lodging', 'lodging')
ON CONFLICT DO NOTHING;

-- Techo de planes (patron V41): erp-free y erp-full incluyen todo modulo activo.
INSERT INTO billing.plan_module (plan_id, module_code)
SELECT p.id, m.code
FROM billing.plan p
JOIN catalog.modules m ON m.is_active = TRUE
WHERE p.code IN ('erp-free', 'erp-full')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- Tipos de habitacion por empresa (aislamiento por company, patron V47)
-- ---------------------------------------------------------------------------
CREATE TABLE erp.room_type (
    id             UUID          PRIMARY KEY,
    tenant_id      UUID          NOT NULL REFERENCES tenant.tenant (id) ON DELETE CASCADE,
    company_id     UUID          NOT NULL REFERENCES tenant.company (id) ON DELETE CASCADE,
    name           VARCHAR(80)   NOT NULL,           -- Simple, Doble, Matrimonial, Suite...
    capacity       INT           NOT NULL DEFAULT 2,
    rate_per_night NUMERIC(10,2) NOT NULL DEFAULT 0,
    description    VARCHAR(300),
    is_active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ   NOT NULL,
    updated_at     TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_room_type_company ON erp.room_type (tenant_id, company_id);

-- ---------------------------------------------------------------------------
-- Habitaciones fisicas por sucursal
-- ---------------------------------------------------------------------------
CREATE TABLE erp.room (
    id           UUID          PRIMARY KEY,
    tenant_id    UUID          NOT NULL REFERENCES tenant.tenant (id) ON DELETE CASCADE,
    company_id   UUID          NOT NULL REFERENCES tenant.company (id) ON DELETE CASCADE,
    branch_id    UUID          NOT NULL REFERENCES tenant.branch (id) ON DELETE CASCADE,
    room_type_id UUID          NOT NULL REFERENCES erp.room_type (id),
    number       VARCHAR(20)   NOT NULL,             -- "101", "2B"
    floor        VARCHAR(20),
    status       VARCHAR(20)   NOT NULL DEFAULT 'AVAILABLE',  -- AVAILABLE, OCCUPIED, CLEANING, MAINTENANCE
    notes        VARCHAR(300),
    is_active    BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ   NOT NULL,
    updated_at   TIMESTAMPTZ   NOT NULL,
    UNIQUE (tenant_id, branch_id, number)
);
CREATE INDEX idx_room_branch ON erp.room (tenant_id, branch_id);

-- ---------------------------------------------------------------------------
-- Estadia / reserva (entidad central). Solape de fechas por habitacion se valida
-- en el service dentro de la transaccion (estados vivos: RESERVED, CHECKED_IN).
-- ---------------------------------------------------------------------------
CREATE TABLE erp.stay (
    id             UUID          PRIMARY KEY,
    tenant_id      UUID          NOT NULL REFERENCES tenant.tenant (id) ON DELETE CASCADE,
    company_id     UUID          NOT NULL REFERENCES tenant.company (id) ON DELETE CASCADE,
    branch_id      UUID          NOT NULL REFERENCES tenant.branch (id) ON DELETE CASCADE,
    room_id        UUID          NOT NULL REFERENCES erp.room (id),
    customer_id    UUID          NOT NULL REFERENCES erp.customer (id),
    check_in_date  DATE          NOT NULL,
    check_out_date DATE          NOT NULL,            -- exclusiva; noches = out - in
    status         VARCHAR(20)   NOT NULL DEFAULT 'RESERVED',  -- RESERVED, CHECKED_IN, CHECKED_OUT, CANCELED, NO_SHOW
    rate_per_night NUMERIC(10,2) NOT NULL DEFAULT 0,  -- copiada del room_type, editable al reservar
    guests_count   INT           NOT NULL DEFAULT 1,
    checked_in_at  TIMESTAMPTZ,
    checked_out_at TIMESTAMPTZ,
    sale_id        UUID          REFERENCES erp.sale (id),     -- venta generada en el check-out
    ticket_code    VARCHAR(10),                       -- codigo corto para busqueda (TicketCodes)
    source         VARCHAR(20)   NOT NULL DEFAULT 'INTERNAL',  -- INTERNAL; ONLINE en fase 2
    notes          VARCHAR(500),
    created_at     TIMESTAMPTZ   NOT NULL,
    updated_at     TIMESTAMPTZ   NOT NULL,
    CHECK (check_out_date > check_in_date)
);
CREATE INDEX idx_stay_agenda ON erp.stay (tenant_id, branch_id, check_in_date, check_out_date);
CREATE INDEX idx_stay_room   ON erp.stay (tenant_id, room_id, status);

-- ---------------------------------------------------------------------------
-- Registro de huespedes (ficha obligatoria, DS 001-2015-MINCETUR)
-- ---------------------------------------------------------------------------
CREATE TABLE erp.stay_guest (
    id          UUID          PRIMARY KEY,
    tenant_id   UUID          NOT NULL REFERENCES tenant.tenant (id) ON DELETE CASCADE,
    stay_id     UUID          NOT NULL REFERENCES erp.stay (id) ON DELETE CASCADE,
    full_name   VARCHAR(120)  NOT NULL,
    doc_type    VARCHAR(10)   NOT NULL DEFAULT 'DNI',  -- DNI, CE, PASAPORTE
    doc_number  VARCHAR(20)   NOT NULL,
    nationality VARCHAR(60)   NOT NULL DEFAULT 'Peru',
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_stay_guest_stay ON erp.stay_guest (tenant_id, stay_id);

-- ---------------------------------------------------------------------------
-- Cargos a la habitacion (minibar, lavanderia...); se liquidan en el check-out
-- como lineas de la venta POS. product_id opcional: descuenta stock al venderse.
-- ---------------------------------------------------------------------------
CREATE TABLE erp.stay_charge (
    id          UUID          PRIMARY KEY,
    tenant_id   UUID          NOT NULL REFERENCES tenant.tenant (id) ON DELETE CASCADE,
    stay_id     UUID          NOT NULL REFERENCES erp.stay (id) ON DELETE CASCADE,
    product_id  UUID          REFERENCES erp.product (id),
    description VARCHAR(160)  NOT NULL,
    quantity    NUMERIC(10,2) NOT NULL DEFAULT 1,
    unit_price  NUMERIC(10,2) NOT NULL DEFAULT 0,
    created_by  UUID,                                 -- trabajador, sin FK (como sale.created_by)
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_stay_charge_stay ON erp.stay_charge (tenant_id, stay_id);

-- ---------------------------------------------------------------------------
-- Lineas de venta de precio libre (sin producto): el check-out liquida noches y
-- cargos como una venta POS normal. product_id pasa a ser opcional y la linea
-- libre lleva su propia descripcion (el ticket la pinta tal cual).
-- ---------------------------------------------------------------------------
ALTER TABLE erp.sale_item ALTER COLUMN product_id DROP NOT NULL;
ALTER TABLE erp.sale_item ADD COLUMN IF NOT EXISTS description VARCHAR(160);

-- ---------------------------------------------------------------------------
-- Permisos del modulo (patron V39)
-- ---------------------------------------------------------------------------
INSERT INTO auth.permission (id, code, description, created_at, updated_at)
SELECT gen_random_uuid(), c.code, c.description, now(), now()
FROM (VALUES
    ('lodging:read',   'Ver habitaciones, reservas y huespedes'),
    ('lodging:manage', 'Gestionar reservas, check-in/out, cargos y habitaciones')
) AS c(code, description)
ON CONFLICT (code) DO NOTHING;

-- Admin global (staff) recibe todo.
INSERT INTO auth.role_permission (role_id, permission_id)
SELECT ro.id, pe.id
FROM auth.role ro
JOIN auth.permission pe ON pe.code IN ('lodging:read', 'lodging:manage')
WHERE ro.code = 'admin' AND ro.tenant_id IS NULL
ON CONFLICT DO NOTHING;

-- Los tenant-admin EXISTENTES tambien (los nuevos los reciben via RoleService).
INSERT INTO auth.role_permission (role_id, permission_id)
SELECT ro.id, pe.id
FROM auth.role ro
JOIN auth.permission pe ON pe.code IN ('lodging:read', 'lodging:manage')
WHERE ro.code = 'tenant-admin' AND ro.tenant_id IS NOT NULL
ON CONFLICT DO NOTHING;
