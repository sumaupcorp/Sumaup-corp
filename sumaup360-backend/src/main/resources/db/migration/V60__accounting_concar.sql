-- V60: modulo de CONTABILIDAD (plan de cuentas PCGE + asientos contables) y export a CONCAR.
-- Alcance v1: asientos de VENTA autogenerados (12 / 40 / 70) + asientos manuales, y
-- exportacion del periodo al formato de importacion de CONCAR (cabecera+detalle denormalizado).
-- El layout exacto de CONCAR varia por version/estudio: el separador es configurable.

-- Plan de cuentas. company_id NULL = plantilla global PCGE (compartida por todos).
-- Cada empresa puede ademas crear cuentas propias (tenant_id + company_id).
CREATE TABLE erp.account (
    id            UUID PRIMARY KEY,
    tenant_id     UUID,
    company_id    UUID,
    code          VARCHAR(20) NOT NULL,
    name          VARCHAR(200) NOT NULL,
    account_class SMALLINT,
    nature        VARCHAR(10),
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_account_scope ON erp.account (tenant_id, company_id);
CREATE INDEX idx_account_code ON erp.account (code);

-- Configuracion contable por empresa: cuentas por defecto + subdiarios + formato CONCAR.
CREATE TABLE erp.accounting_config (
    id                 UUID PRIMARY KEY,
    tenant_id          UUID NOT NULL,
    company_id         UUID NOT NULL,
    subdiario_ventas   VARCHAR(4)  NOT NULL DEFAULT '14',
    subdiario_compras  VARCHAR(4)  NOT NULL DEFAULT '02',
    subdiario_diario   VARCHAR(4)  NOT NULL DEFAULT '01',
    cuenta_por_cobrar  VARCHAR(20) NOT NULL DEFAULT '1212',
    cuenta_ventas      VARCHAR(20) NOT NULL DEFAULT '7011',
    cuenta_ventas_serv VARCHAR(20) NOT NULL DEFAULT '7041',
    cuenta_igv         VARCHAR(20) NOT NULL DEFAULT '40111',
    cuenta_caja        VARCHAR(20) NOT NULL DEFAULT '101',
    moneda             VARCHAR(2)  NOT NULL DEFAULT 'MN',
    concar_separator   VARCHAR(4)  NOT NULL DEFAULT '|',
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_accounting_config UNIQUE (tenant_id, company_id)
);

-- Asiento contable (cabecera).
CREATE TABLE erp.journal_entry (
    id          UUID PRIMARY KEY,
    tenant_id   UUID NOT NULL,
    company_id  UUID NOT NULL,
    entry_date  DATE NOT NULL,
    subdiario   VARCHAR(4) NOT NULL,
    correlativo VARCHAR(20),
    glosa       VARCHAR(200),
    moneda      VARCHAR(2) NOT NULL DEFAULT 'MN',
    tipo_cambio NUMERIC(10,3),
    source      VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    source_id   UUID,
    total_debe  NUMERIC(14,2) NOT NULL DEFAULT 0,
    total_haber NUMERIC(14,2) NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_journal_entry_period ON erp.journal_entry (tenant_id, company_id, entry_date);

-- Detalle del asiento (una linea por cuenta).
CREATE TABLE erp.journal_entry_line (
    id               UUID PRIMARY KEY,
    tenant_id        UUID NOT NULL,
    journal_entry_id UUID NOT NULL,
    account_code     VARCHAR(20) NOT NULL,
    glosa            VARCHAR(200),
    debe             NUMERIC(14,2) NOT NULL DEFAULT 0,
    haber            NUMERIC(14,2) NOT NULL DEFAULT 0,
    doc_tipo_sunat   VARCHAR(2),
    doc_serie_numero VARCHAR(40),
    doc_fecha        DATE,
    tercero_doc_tipo VARCHAR(2),
    tercero_doc_num  VARCHAR(20),
    tercero_nombre   VARCHAR(200),
    orden            INT NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_journal_line_entry ON erp.journal_entry_line (journal_entry_id);

-- Permisos.
INSERT INTO auth.permission (id, code, description, created_at, updated_at)
SELECT gen_random_uuid(), c.code, c.description, now(), now()
FROM (VALUES
    ('accounting:read',   'Ver contabilidad (plan de cuentas y asientos)'),
    ('accounting:manage', 'Gestionar contabilidad (asientos y configuracion)'),
    ('accounting:export', 'Exportar contabilidad a CONCAR')
) AS c(code, description)
ON CONFLICT (code) DO NOTHING;

INSERT INTO auth.role_permission (role_id, permission_id)
SELECT ro.id, pe.id
FROM auth.role ro
JOIN auth.permission pe ON pe.code IN ('accounting:read', 'accounting:manage', 'accounting:export')
WHERE ro.code = 'admin' AND ro.tenant_id IS NULL
ON CONFLICT DO NOTHING;

-- A todo rol que ya vea ventas (sale:read) se le da lectura/manage/export contable.
INSERT INTO auth.role_permission (role_id, permission_id)
SELECT DISTINCT rp.role_id, pe.id
FROM auth.role_permission rp
JOIN auth.permission src ON src.id = rp.permission_id AND src.code = 'sale:read'
JOIN auth.permission pe  ON pe.code IN ('accounting:read', 'accounting:manage', 'accounting:export')
ON CONFLICT DO NOTHING;

-- Plantilla global PCGE (compacta, cuentas mas usadas). tenant_id/company_id NULL.
INSERT INTO erp.account (id, tenant_id, company_id, code, name, account_class, nature, active, created_at, updated_at)
SELECT gen_random_uuid(), NULL, NULL, c.code, c.name, c.klass, c.nature, TRUE, now(), now()
FROM (VALUES
    ('10',    'Efectivo y equivalentes de efectivo', 1, 'DEUDORA'),
    ('101',   'Caja', 1, 'DEUDORA'),
    ('104',   'Cuentas corrientes en instituciones financieras', 1, 'DEUDORA'),
    ('12',    'Cuentas por cobrar comerciales - terceros', 1, 'DEUDORA'),
    ('1212',  'Facturas, boletas y otros comprobantes por cobrar - emitidas en cartera', 1, 'DEUDORA'),
    ('20',    'Mercaderias', 2, 'DEUDORA'),
    ('40',    'Tributos, contraprestaciones y aportes al sistema de pensiones y de salud por pagar', 4, 'ACREEDORA'),
    ('40111', 'IGV - Cuenta propia', 4, 'ACREEDORA'),
    ('42',    'Cuentas por pagar comerciales - terceros', 4, 'ACREEDORA'),
    ('4212',  'Facturas, boletas y otros comprobantes por pagar - emitidas', 4, 'ACREEDORA'),
    ('60',    'Compras', 6, 'DEUDORA'),
    ('601',   'Mercaderias', 6, 'DEUDORA'),
    ('63',    'Gastos de servicios prestados por terceros', 6, 'DEUDORA'),
    ('70',    'Ventas', 7, 'ACREEDORA'),
    ('7011',  'Mercaderias - terceros', 7, 'ACREEDORA'),
    ('7041',  'Servicios - terceros', 7, 'ACREEDORA')
) AS c(code, name, klass, nature)
ON CONFLICT DO NOTHING;
