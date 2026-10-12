-- V62: Diferenciación de solicitudes QR vs Corporativas
ALTER TABLE app.receipt_request
    ADD COLUMN origen VARCHAR(30) NOT NULL DEFAULT 'QR'; -- 'QR' | 'CORPORATIVO'

CREATE INDEX idx_request_origen ON app.receipt_request (origen);
CREATE INDEX idx_request_taxista_origen ON app.receipt_request (taxista_user_id, origen, created_at);
