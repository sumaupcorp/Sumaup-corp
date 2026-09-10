-- V41: los planes que dan "todos los modulos" (erp-free, erp-full) incluyen los modulos
-- nuevos de V38 (patients, custom-orders). V14/V16 fijaron la lista con un snapshot de
-- catalog.modules de aquel momento, asi que cada modulo nuevo debe sumarse al techo.
-- erp-basico se mantiene solo-core a proposito.

INSERT INTO billing.plan_module (plan_id, module_code)
SELECT p.id, m.code
FROM billing.plan p
JOIN catalog.modules m ON m.is_active = TRUE
WHERE p.code IN ('erp-free', 'erp-full')
ON CONFLICT DO NOTHING;
