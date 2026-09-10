-- Modo de autenticacion de la Clave SOL elegido por la persona:
--   'ruc' = RUC + usuario SOL + clave
--   'dni' = DNI + clave
-- Necesario para que el backoffice/RPA sepa como re-iniciar sesion en SUNAT.
ALTER TABLE app.person_profile ADD COLUMN IF NOT EXISTS sol_doc_mode varchar(3);
