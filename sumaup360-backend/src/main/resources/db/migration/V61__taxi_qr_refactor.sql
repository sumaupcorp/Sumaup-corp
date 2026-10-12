-- Eliminar la tabla de códigos QR estáticos aleatorios,
-- ya que ahora el token del QR público será simplemente el userId del taxista.
DROP TABLE IF EXISTS app.qr_code;

-- Eliminar la columna qr_token de la tabla de solicitudes de comprobantes,
-- dado que es redundante con taxista_user_id (el cual ahora también es el token).
ALTER TABLE app.receipt_request DROP COLUMN IF EXISTS qr_token;
