-- V36: la temperatura se mapea como double en la entidad; alinea el tipo de columna para que
-- la validacion de esquema de Hibernate pase (numeric -> double precision).

ALTER TABLE app.ai_prompt
    ALTER COLUMN temperature TYPE double precision USING temperature::double precision,
    ALTER COLUMN temperature SET DEFAULT 0.4;
