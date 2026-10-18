-- Seed EXCLUSIVO para la base pulsepass_e2e. Nunca ejecutar en desarrollo/QA/producción.
-- Requiere variables psql: staff_pw, admin_pw, inactive_pw (no se versionan).
-- Ejecutar DESPUÉS de que Flyway haya aplicado V1..V4 (arrancar la app una vez).
-- Pensado para una base limpia; para repetir, recrear pulsepass_e2e.

\set ON_ERROR_STOP on
BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Venue activo (las migraciones no siembran venues)
INSERT INTO venues (code, name, city, address, capacity, active)
VALUES ('E2E-VENUE-01', 'Venue E2E', 'Santa Marta', 'Calle 1 # 1-01', 500, TRUE);

-- Eventos (fecha futura relativa al momento del seed)
INSERT INTO events (event_code, name, description, category, status, event_date, minimum_age, venue_id)
SELECT v.code_ev, v.name, 'Fixture E2E', 'MUSIC', v.status,
       CURRENT_TIMESTAMP + INTERVAL '1 year', 0,
    (SELECT id FROM venues WHERE code = 'E2E-VENUE-01')
FROM (VALUES
    ('E2E-DRAFT-01',  'E2E Borrador',      'DRAFT'),
    ('E2E-PUB-01',    'E2E Publicado',     'PUBLISHED'),
    ('E2E-TOPUB-01',  'E2E Por publicar',  'DRAFT')
    ) AS v(code_ev, name, status);

-- Artista de la V2 asociado al evento publicado (para by-artist público)
INSERT INTO event_artists (event_id, artist_id)
SELECT e.id, a.id
FROM events e, artists a
WHERE e.event_code = 'E2E-PUB-01' AND a.stage_name = 'Solar Beat';

-- Usuarios privilegiados e inactivo
INSERT INTO users (username, email, active) VALUES
                                                ('e2e_staff',    'e2e.staff@example.test',    TRUE),
                                                ('e2e_admin',    'e2e.admin@example.test',    TRUE),
                                                ('e2e_inactive', 'e2e.inactive@example.test', FALSE);

INSERT INTO user_profiles (first_name, last_name, birth_date, user_id)
SELECT 'E2E', initcap(replace(username, 'e2e_', '')), DATE '1990-01-01', id
FROM users WHERE username IN ('e2e_staff', 'e2e_admin', 'e2e_inactive');

INSERT INTO user_credentials (user_id, password_hash, password_changed_at)
SELECT id, crypt(:'staff_pw',    gen_salt('bf', 10)), CURRENT_TIMESTAMP FROM users WHERE username = 'e2e_staff';
INSERT INTO user_credentials (user_id, password_hash, password_changed_at)
SELECT id, crypt(:'admin_pw',    gen_salt('bf', 10)), CURRENT_TIMESTAMP FROM users WHERE username = 'e2e_admin';
INSERT INTO user_credentials (user_id, password_hash, password_changed_at)
SELECT id, crypt(:'inactive_pw', gen_salt('bf', 10)), CURRENT_TIMESTAMP FROM users WHERE username = 'e2e_inactive';

INSERT INTO user_roles (user_id, role)
SELECT id, CASE username WHEN 'e2e_staff' THEN 'STAFF'
                         WHEN 'e2e_admin' THEN 'ADMIN'
                         ELSE 'USER' END
FROM users WHERE username IN ('e2e_staff', 'e2e_admin', 'e2e_inactive');

COMMIT;