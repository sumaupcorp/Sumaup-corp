-- V20: ruta del archivo del recibo en object storage (Firebase Storage).
-- La BD guarda solo la ruta (ej. receipts/{userId}/{receiptId}.jpg); los bytes viven en Storage.
-- El acceso se hace con signed URL de corta duracion generado por el backend.

ALTER TABLE app.receipt
    ADD COLUMN file_path VARCHAR(500);
