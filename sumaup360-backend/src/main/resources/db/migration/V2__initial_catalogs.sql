-- V2: catalogos minimos compartidos en el schema catalog.
-- Datos semilla reales del contexto peruano. snake_case, PK identity, IF NOT EXISTS.
-- Estos catalogos son transversales: alimentan tanto Personas (app) como Negocios (erp).

-- ---------------------------------------------------------------------------
-- Monedas
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS catalog.currencies (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code        VARCHAR(3)   NOT NULL UNIQUE,   -- ISO 4217
    name        VARCHAR(60)  NOT NULL,
    symbol      VARCHAR(8)   NOT NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE
);

INSERT INTO catalog.currencies (code, name, symbol) VALUES
    ('PEN', 'Sol peruano', 'S/'),
    ('USD', 'Dolar estadounidense', '$')
ON CONFLICT (code) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Tipos de comprobante / documento
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS catalog.document_types (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code        VARCHAR(20)  NOT NULL UNIQUE,
    name        VARCHAR(80)  NOT NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE
);

INSERT INTO catalog.document_types (code, name) VALUES
    ('FACTURA', 'Factura'),
    ('BOLETA', 'Boleta de venta'),
    ('RHE', 'Recibo por honorarios electronico')
ON CONFLICT (code) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Rubros de negocio (Linea Negocios / ERP)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS catalog.business_types (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code        VARCHAR(40)  NOT NULL UNIQUE,
    name        VARCHAR(80)  NOT NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE
);

INSERT INTO catalog.business_types (code, name) VALUES
    ('restaurant', 'Restaurante'),
    ('pharmacy', 'Botica / Farmacia'),
    ('hardware', 'Ferreteria'),
    ('grocery', 'Bodega'),
    ('minimarket', 'Minimarket'),
    ('bookstore', 'Libreria'),
    ('beauty', 'Salon de belleza'),
    ('laundry', 'Lavanderia')
ON CONFLICT (code) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Verticales / sub-rubros (extienden un rubro; ej. restaurante -> cevicheria)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS catalog.verticals (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    business_type_id    BIGINT       NOT NULL REFERENCES catalog.business_types (id),
    code                VARCHAR(40)  NOT NULL UNIQUE,
    name                VARCHAR(80)  NOT NULL,
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE
);

INSERT INTO catalog.verticals (business_type_id, code, name)
SELECT bt.id, v.code, v.name
FROM (VALUES
    ('restaurant', 'cevicheria', 'Cevicheria'),
    ('restaurant', 'polleria', 'Polleria'),
    ('restaurant', 'chifa', 'Chifa'),
    ('restaurant', 'pasteleria', 'Pasteleria'),
    ('restaurant', 'cafeteria', 'Cafeteria')
) AS v(business_type_code, code, name)
JOIN catalog.business_types bt ON bt.code = v.business_type_code
ON CONFLICT (code) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Modulos del sistema (se habilitan por rubro / plan; no se duplica codigo)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS catalog.modules (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code        VARCHAR(40)  NOT NULL UNIQUE,
    name        VARCHAR(80)  NOT NULL,
    is_core     BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE
);

INSERT INTO catalog.modules (code, name, is_core) VALUES
    ('company', 'Empresa', TRUE),
    ('branch', 'Sucursales', TRUE),
    ('users', 'Usuarios y roles', TRUE),
    ('product', 'Productos', TRUE),
    ('inventory', 'Inventario', TRUE),
    ('sale', 'Ventas', TRUE),
    ('pos', 'Caja / POS', TRUE),
    ('customer', 'Clientes', TRUE),
    ('invoice', 'Comprobantes', TRUE),
    ('supplier', 'Proveedores', TRUE),
    ('report', 'Reportes', TRUE)
ON CONFLICT (code) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Segmentos de persona (Linea Personas / app)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS catalog.person_segments (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code        VARCHAR(40)  NOT NULL UNIQUE,
    name        VARCHAR(80)  NOT NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE
);

INSERT INTO catalog.person_segments (code, name) VALUES
    ('taxi', 'Taxista'),
    ('delivery', 'Repartidor de delivery'),
    ('landlord', 'Arrendador'),
    ('professional-ruc', 'Profesional con RUC'),
    ('honorarios-4ta', 'Recibos por honorarios (4ta categoria)'),
    ('nuevo-rus', 'Nuevo RUS')
ON CONFLICT (code) DO NOTHING;
