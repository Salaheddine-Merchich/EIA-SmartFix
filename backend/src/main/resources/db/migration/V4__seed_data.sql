-- V4: Seed demo data
INSERT INTO users (id, email, password_hash, role, nom_prenom, actif)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'admin@ocp.ma',
     '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G2oX.YwKQgK.O', 'ADMIN', 'Admin OCP', TRUE),
    ('22222222-2222-2222-2222-222222222222', 'responsable@ocp.ma',
     '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G2oX.YwKQgK.O', 'RESPONSABLE_EIA', 'Karim Benali', TRUE),
    ('33333333-3333-3333-3333-333333333333', 'technicien@ocp.ma',
     '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G2oX.YwKQgK.O', 'TECHNICIEN', 'Youssef Alami', TRUE);

-- Default password for all demo users: Password123!

INSERT INTO equipment (id, code, designation, famille, zone, constructeur, mise_en_service)
VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'MOT-001', 'Moteur principal convoyeur A', 'Moteurs', 'Zone A', 'Siemens', '2018-03-15'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'VAR-012', 'Variateur de vitesse ligne 2', 'Automatisme', 'Zone B', 'ABB', '2019-07-22'),
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'CAP-045', 'Capteur de niveau silo 3', 'Instrumentation', 'Zone C', 'Endress+Hauser', '2020-01-10');
