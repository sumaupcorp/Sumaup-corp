-- V49: gestion completa de caja (modelo peruano de caja chica / POS).
-- 1) Arqueo al cierre: efectivo esperado (calculado), efectivo contado y diferencia
--    (sobrante/faltante), con responsable y notas.
-- 2) Movimientos de caja: ingresos y salidas de efectivo fuera de ventas
--    (gastos menores, pagos a proveedor, retiros del dueno, remesas al banco).
-- 3) Metodo de pago por venta: solo el efectivo suma al arqueo fisico;
--    tarjeta / Yape / Plin / transferencia se cuadran contra sus vouchers.

ALTER TABLE erp.cash_session ADD COLUMN closed_by       UUID;
ALTER TABLE erp.cash_session ADD COLUMN expected_amount NUMERIC(12,2);
ALTER TABLE erp.cash_session ADD COLUMN difference      NUMERIC(12,2);
ALTER TABLE erp.cash_session ADD COLUMN notes           VARCHAR(300);

ALTER TABLE erp.sale ADD COLUMN payment_method VARCHAR(20) NOT NULL DEFAULT 'CASH';

CREATE TABLE erp.cash_movement (
    id              UUID          PRIMARY KEY,
    tenant_id       UUID          NOT NULL REFERENCES tenant.tenant (id) ON DELETE CASCADE,
    cash_session_id UUID          NOT NULL REFERENCES erp.cash_session (id) ON DELETE CASCADE,
    type            VARCHAR(10)   NOT NULL,   -- INCOME (entra efectivo), EXPENSE (sale efectivo)
    category        VARCHAR(30)   NOT NULL,   -- ver CashMovementCategory (COMPRA, PROVEEDOR, RETIRO, ...)
    concept         VARCHAR(200)  NOT NULL,   -- descripcion libre: "compra de bolsas", "pago mototaxi"
    amount          NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    created_by      UUID,
    created_at      TIMESTAMPTZ   NOT NULL,
    updated_at      TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_cash_movement_session ON erp.cash_movement (cash_session_id);
CREATE INDEX idx_cash_movement_tenant  ON erp.cash_movement (tenant_id);
