-- V10: Expand RAG knowledge base with 4 new industrial scenarios (idempotent)

INSERT INTO equipment (id, code, designation, famille, zone, constructeur, mise_en_service)
VALUES
    ('e1010101-1010-1010-1010-101010101010', 'HYDR-015', 'Vérin hydraulique principal presse 3', 'Hydraulique', 'Zone E', 'Bosch Rexroth', '2020-03-15'),
    ('e2020202-2020-2020-2020-202020202020', 'PLC-021', 'Automate Siemens S7-1500 ligne de conditionnement', 'Automatisme', 'Zone F', 'Siemens', '2019-08-22'),
    ('e3030303-3030-3030-3030-303030303030', 'CVY-003', 'Convoyeur principal transport minerai', 'Convoyage', 'Zone G', 'Continental', '2018-12-10'),
    ('e4040404-4040-4040-4040-404040404040', 'ELC-007', 'Contacteur moteur 75kW broyeur secondaire', 'Électricité', 'Zone H', 'Schneider Electric', '2021-07-30')
ON CONFLICT (id) DO NOTHING;

INSERT INTO failures (id, equipment_id, date_heure, criticite, zone_service, responsable_id, declarant_id, statut, description_initiale, code_defaut)
VALUES
    ('f7777777-7777-7777-7777-777777777777', 'e1010101-1010-1010-1010-101010101010',
     '2026-03-15 11:20:00', 'CRITIQUE', 'Zone E', '22222222-2222-2222-2222-222222222222',
     '11111111-1111-1111-1111-111111111111', 'RESOLUE', 'Vérin hydraulique fuite importante, perte pression circuit', 'H001'),
    ('f8888888-8888-8888-8888-888888888888', 'e2020202-2020-2020-2020-202020202020',
     '2026-03-20 14:45:00', 'HAUTE', 'Zone F', '22222222-2222-2222-2222-222222222222',
     '11111111-1111-1111-1111-111111111111', 'CLOTUREE', 'Communication Profinet perdue entre automate et variateur ABB', 'P001'),
    ('f9999999-9999-9999-9999-999999999999', 'e3030303-3030-3030-3030-303030303030',
     '2026-03-25 08:30:00', 'MOYENNE', 'Zone G', '22222222-2222-2222-2222-222222222222',
     '11111111-1111-1111-1111-111111111111', 'RESOLUE', 'Courroie convoyeur glisse sous charge, tension insuffisante', 'C001'),
    ('fabbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'e4040404-4040-4040-4040-404040404040',
     '2026-04-02 16:10:00', 'HAUTE', 'Zone H', '22222222-2222-2222-2222-222222222222',
     '11111111-1111-1111-1111-111111111111', 'RESOLUE', 'Contacteur moteur broyeur ne ferme pas au démarrage', 'EL01')
ON CONFLICT (id) DO NOTHING;

INSERT INTO interventions (id, failure_id, technicien_id, description, symptomes, cause_racine,
                           analyse_technique, actions_correctives, pieces_remplacees,
                           duree_arret_minutes, temps_intervention_minutes, statut_validation,
                           validateur_id, date_validation, commentaire_validation)
VALUES
    ('a8888888-8888-8888-8888-888888888888', 'f7777777-7777-7777-7777-777777777777',
     '33333333-3333-3333-3333-333333333333',
     'Réparation fuite vérin hydraulique presse 3',
     'Fuite huile importante au niveau du joint de tige, pression chutée de 210 bar à 120 bar',
     'Usure prématurée joint de tige par contamination huile',
     'Test pression : chute progressive. Inspection joint : rainure d''étanchéité endommagée. Analyse huile : particules métalliques.',
     'Remplacement joint de tige, filtration huile, contrôle pression système',
     'Kit joint vérin Bosch Rexroth, filtre huile 25µm',
     180, 120, 'VALIDEE', '22222222-2222-2222-2222-222222222222', '2026-03-16 09:30:00', 'Intervention urgente traitée'),

    ('a9999999-9999-9999-9999-999999999999', 'f8888888-8888-8888-8888-888888888888',
     '33333333-3333-3333-3333-333333333333',
     'Rétablissement communication Profinet automate-variateur',
     'Défaut réseau Profinet, variateur ABB en état "Bus Off", arrêt ligne production',
     'Câble Profinet endommagé par pincement lors maintenance préventive',
     'Test continuité : rupture conducteur D+ au niveau connecteur M12. Vérification topology Profinet : node manquant.',
     'Remplacement câble Profinet, test communication, reconfiguration adresse IP variateur',
     'Câble Profinet 5m blindé M12, connecteur M12',
     240, 90, 'VALIDEE', '22222222-2222-2222-2222-222222222222', '2026-03-21 10:15:00', 'Communication rétablie'),

    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaab', 'f9999999-9999-9999-9999-999999999999',
     '33333333-3333-3333-3333-333333333333',
     'Retension courroie convoyeur principal minerai',
     'Glissement courroie sous charge pleine, traces d''usure sur poulie motrice',
     'Détente courroie par fluage normal, tension de service insuffisante',
     'Mesure tension courroie : 180N/cm au lieu des 220N/cm spécifiés. Contrôle alignement poulies OK.',
     'Retension courroie selon procédure constructeur, contrôle couple moteur',
     NULL,
     45, 30, 'VALIDEE', '22222222-2222-2222-2222-222222222222', '2026-03-25 12:00:00', NULL),

    ('abbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'fabbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
     '33333333-3333-3333-3333-333333333333',
     'Remplacement contacteur principal moteur broyeur',
     'Contacteur ne ferme pas, voyant défaut allumé, moteur 75kW ne démarre pas',
     'Contacts principaux soudés par surintensité lors démarrage à vide prolongé',
     'Test isolement contacts : résistance < 1 ohm entre phases. Inspection visuelle : contacts collés, traces d''arc électrique.',
     'Remplacement contacteur, vérification protection moteur, test démarrage sous charge',
     'Contacteur Schneider LC1D80 80A, auxiliaires LA1DN11',
     90, 60, 'VALIDEE', '22222222-2222-2222-2222-222222222222', '2026-04-03 08:45:00', 'Démarrage normalisé')
ON CONFLICT (id) DO NOTHING;