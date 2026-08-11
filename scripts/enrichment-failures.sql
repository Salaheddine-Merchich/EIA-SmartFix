-- Script d'enrichissement pannes pour le RAG EIA SmartFix  
-- Crée 20 pannes variées avec codes défaut réalistes

-- Pannes Instrumentation
INSERT INTO failures (id, equipment_id, date_heure, criticite, zone_service, responsable_id, declarant_id, statut, description_initiale, code_defaut)
VALUES
    -- PTI-056 (Capteur pression)
    ('f1010101-1010-1010-1010-101010101010', '10101010-1010-1010-1010-101010101010', '2026-07-15 09:15:00', 'HAUTE', 'Zone A', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'RESOLUE', 'Capteur pression signal erratique, valeurs oscillent entre 0 et 250 bars', 'S001'),
    ('f1010102-1010-1010-1010-101010101010', '10101010-1010-1010-1010-101010101010', '2026-07-20 14:30:00', 'MOYENNE', 'Zone A', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'RESOLUE', 'Dérive étalonnage capteur pression, écart +15% par rapport référence', 'S002'),
    
    -- FIT-078 (Débitmètre) 
    ('f2020201-2020-2020-2020-202020202020', '20202020-2020-2020-2020-202020202020', '2026-07-18 11:45:00', 'CRITIQUE', 'Zone B', '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', 'RESOLUE', 'Débitmètre aucun signal, affichage "---" sur superviseur', 'F003'),
    ('f2020202-2020-2020-2020-202020202020', '20202020-2020-2020-2020-202020202020', '2026-07-25 16:20:00', 'HAUTE', 'Zone B', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'RESOLUE', 'Mesure débit bloquée à 0 m3/h malgré circulation visible', 'F004'),
    
    -- AIT-134 (Analyseur pH)
    ('f3030301-3030-3030-3030-303030303030', '30303030-3030-3030-3030-303030303030', '2026-07-22 08:30:00', 'MOYENNE', 'Zone C', '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', 'RESOLUE', 'Analyseur pH lecture instable, valeur fluctue entre 6.8 et 8.2', 'A001')
ON CONFLICT (id) DO NOTHING;

-- Pannes Automatisme
INSERT INTO failures (id, equipment_id, date_heure, criticite, zone_service, responsable_id, declarant_id, statut, description_initiale, code_defaut)
VALUES
    -- PLC-067 (Allen-Bradley)
    ('f4040401-4040-4040-4040-404040404040', '40404040-4040-4040-4040-404040404040', '2026-07-16 13:20:00', 'CRITIQUE', 'Zone A', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'RESOLUE', 'Automate PLC communication Ethernet/IP perdue avec superviseur', 'P002'),
    ('f4040402-4040-4040-4040-404040404040', '40404040-4040-4040-4040-404040404040', '2026-07-28 10:15:00', 'HAUTE', 'Zone A', '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', 'RESOLUE', 'Module E/S analogiques défaillant, signaux 4-20mA incorrects', 'M005'),
    
    -- DI-089 (Module Modbus)
    ('f5050501-5050-5050-5050-505050505050', '50505050-5050-5050-5050-505050505050', '2026-07-19 15:40:00', 'HAUTE', 'Zone B', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'RESOLUE', 'Module Modbus TCP timeout sur registres, communication sporadique', 'M006'),
    
    -- UPS-012 (Onduleur)
    ('f6060601-6060-6060-6060-606060606060', '60606060-6060-6060-6060-606060606060', '2026-07-21 07:50:00', 'CRITIQUE', 'Zone C', '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', 'RESOLUE', 'Onduleur alarme batterie défaillante, autonomie réduite à 3 minutes', 'E007'),
    ('f6060602-6060-6060-6060-606060606060', '60606060-6060-6060-6060-606060606060', '2026-07-30 12:25:00', 'MOYENNE', 'Zone C', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'RESOLUE', 'Surtension réseau détectée, onduleur passe en mode bypass', 'E008')
ON CONFLICT (id) DO NOTHING;

-- Pannes Mécanique  
INSERT INTO failures (id, equipment_id, date_heure, criticite, zone_service, responsable_id, declarant_id, statut, description_initiale, code_defaut)
VALUES
    -- RED-045 (Réducteur)
    ('f7070701-7070-7070-7070-707070707070', '70707070-7070-7070-7070-707070707070', '2026-07-17 14:10:00', 'HAUTE', 'Zone D', '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', 'RESOLUE', 'Réducteur vibrations anormales et bruit métallique, huile chaude', 'M009'),
    ('f7070702-7070-7070-7070-707070707070', '70707070-7070-7070-7070-707070707070', '2026-08-01 09:35:00', 'CRITIQUE', 'Zone D', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'RESOLUE', 'Fuite huile importante réducteur, niveau bas critiq ue', 'M010'),
    
    -- VEN-123 (Ventilateur)
    ('f8080801-8080-8080-8080-808080808080', '80808080-8080-8080-8080-808080808080', '2026-07-23 11:20:00', 'MOYENNE', 'Zone A', '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', 'RESOLUE', 'Ventilateur débit insuffisant, performance réduite de 40%', 'M011'),
    ('f8080802-8080-8080-8080-808080808080', '80808080-8080-8080-8080-808080808080', '2026-08-03 16:45:00', 'HAUTE', 'Zone A', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'RESOLUE', 'Courroie ventilateur détendue, glissement sous charge', 'M012')
ON CONFLICT (id) DO NOTHING;

-- Pannes Électricité
INSERT INTO failures (id, equipment_id, date_heure, criticite, zone_service, responsable_id, declarant_id, statut, description_initiale, code_defaut)
VALUES
    -- CTR-234 (Contacteur)
    ('f9090901-9090-9090-9090-909090909090', '90909090-9090-9090-9090-909090909090', '2026-07-24 08:15:00', 'CRITIQUE', 'Zone B', '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', 'RESOLUE', 'Contacteur ne ferme plus, moteur pompe ne démarre pas', 'EL13'),
    ('f9090902-9090-9090-9090-909090909090', '90909090-9090-9090-9090-909090909090', '2026-08-02 13:50:00', 'HAUTE', 'Zone B', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'RESOLUE', 'Contacts contracteur grillés, arc électrique lors manœuvre', 'EL14'),
    
    -- TRF-567 (Transformateur)
    ('fa0a0a01-a0a0-a0a0-a0a0-a0a0a0a0a0a0', 'a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0', '2026-07-26 10:30:00', 'MOYENNE', 'Zone C', '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', 'RESOLUE', 'Transformateur surchauffe, température enroulement +85°C', 'T015'),
    ('fa0a0a02-a0a0-a0a0-a0a0-a0a0a0a0a0a0', 'a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0', '2026-08-04 15:10:00', 'HAUTE', 'Zone C', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'RESOLUE', 'Harmoniques transformateur, déformation onde sinusoïdale', 'T016')
ON CONFLICT (id) DO NOTHING;