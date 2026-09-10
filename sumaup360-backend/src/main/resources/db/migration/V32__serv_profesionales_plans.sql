-- V32: planes para Servicios Profesionales (Recibos por Honorarios + Suspension de 4ta).
-- serv-premium S/ 14.90 mensual. Se activa manual desde el backoffice (como taxi/peya).

INSERT INTO billing.plan (id, code, name, line, price, currency, active, created_at, updated_at)
SELECT gen_random_uuid(), v.code, v.name, 'PERSON', v.price, 'PEN', TRUE, now(), now()
FROM (VALUES
    ('serv-free',    'Servicios Profesionales Free',    0.00),
    ('serv-premium', 'Servicios Profesionales Premium', 14.90)
) AS v(code, name, price)
WHERE NOT EXISTS (SELECT 1 FROM billing.plan p WHERE p.code = v.code);
