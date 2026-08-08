-- V9: Restore demo failures/interventions for RAG, search and dashboard (idempotent)

INSERT INTO equipment (id, code, designation, famille, zone, constructeur, mise_en_service)
VALUES
    ('dddddddd-dddd-dddd-dddd-dddddddddddd', 'POM-008', 'Pompe alimentation circuit mécanique', 'Mécanique', 'Zone D', 'KSB', '2021-06-01')
ON CONFLICT (id) DO NOTHING;

INSERT INTO failures (id, equipment_id, date_heure, criticite, zone_service, responsable_id, declarant_id, statut, description_initiale, code_defaut)
VALUES
    ('f1111111-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
     '2025-11-12 08:15:00', 'HAUTE', 'Zone A', '22222222-2222-2222-2222-222222222222',
     '11111111-1111-1111-1111-111111111111', 'RESOLUE', 'Moteur convoyeur A en surchauffe, arrêt automatique', 'F001'),
    ('f2222222-2222-2222-2222-222222222222', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
     '2025-11-18 14:30:00', 'MOYENNE', 'Zone B', '22222222-2222-2222-2222-222222222222',
     '11111111-1111-1111-1111-111111111111', 'RESOLUE', 'Variateur ABB affiche défaut surintensité ligne 2', 'E001'),
    ('f3333333-3333-3333-3333-333333333333', 'cccccccc-cccc-cccc-cccc-cccccccccccc',
     '2025-12-02 06:45:00', 'CRITIQUE', 'Zone C', '22222222-2222-2222-2222-222222222222',
     '11111111-1111-1111-1111-111111111111', 'CLOTUREE', 'Capteur niveau silo 3 signal erroné, risque débordement', 'S004'),
    ('f4444444-4444-4444-4444-444444444444', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
     '2026-01-08 10:00:00', 'MOYENNE', 'Zone A', '22222222-2222-2222-2222-222222222222',
     '11111111-1111-1111-1111-111111111111', 'EN_COURS', 'Vibrations anormales moteur convoyeur A', 'F002'),
    ('f5555555-5555-5555-5555-555555555555', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
     '2026-01-22 16:20:00', 'FAIBLE', 'Zone B', '22222222-2222-2222-2222-222222222222',
     '11111111-1111-1111-1111-111111111111', 'OUVERTE', 'Communication Modbus intermittente variateur ligne 2', 'E012'),
    ('f6666666-6666-6666-6666-666666666666', 'dddddddd-dddd-dddd-dddd-dddddddddddd',
     '2026-02-10 09:30:00', 'HAUTE', 'Zone D', '22222222-2222-2222-2222-222222222222',
     '11111111-1111-1111-1111-111111111111', 'RESOLUE', 'Pompe mécanique fuite joint arbre, perte de débit', 'M003')
ON CONFLICT (id) DO NOTHING;

INSERT INTO interventions (id, failure_id, technicien_id, description, symptomes, cause_racine,
                           analyse_technique, actions_correctives, pieces_remplacees,
                           duree_arret_minutes, temps_intervention_minutes, statut_validation,
                           validateur_id, date_validation, commentaire_validation)
VALUES
    ('a1111111-1111-1111-1111-111111111111', 'f1111111-1111-1111-1111-111111111111',
     '33333333-3333-3333-3333-333333333333',
     'Intervention surchauffe moteur convoyeur A',
     'Température carter > 85°C, ventilateur bruyant, odeur isolation',
     'Roulement arrière grippé par manque de graissage',
     'Mesure vibration ISO 10816 dépassement zone C. Inspection palier arrière : jeu axial excessif.',
     'Remplacement roulement SKF 6312, graissage paliers, contrôle alignement',
     'Roulement SKF 6312, graisse LGMT2',
     240, 180, 'VALIDEE', '22222222-2222-2222-2222-222222222222', '2025-11-13 09:00:00', 'Intervention conforme'),

    ('a2222222-2222-2222-2222-222222222222', 'f2222222-2222-2222-2222-222222222222',
     '33333333-3333-3333-3333-333333333333',
     'Correction défaut surintensité variateur ABB ACS880',
     'Code E001 au démarrage, courant phase R élevé',
     'Paramétrage limite courant moteur incorrect après remplacement moteur',
     'Vérification câblage et paramètres 99.06/99.09. Courant nominal moteur 42A vs param 32A.',
     'Ajustement paramètres moteur, reset défauts, test charge nominale',
     NULL,
     90, 45, 'VALIDEE', '22222222-2222-2222-2222-222222222222', '2025-11-19 08:30:00', NULL),

    ('a3333333-3333-3333-3333-333333333333', 'f3333333-3333-3333-3333-333333333333',
     '33333333-3333-3333-3333-333333333333',
     'Diagnostic capteur niveau silo 3 Endress+Hauser',
     'Mesure figée à 100%, alarme niveau haut malgré vidange',
     'Encrassement sonde radar par poussière phosphate',
     'Inspection visuelle : dépôt calcaire sur antenne. Test loop 4-20mA OK côté automate.',
     'Nettoyage sonde, recalibrage distance vide/plein, remise en service',
     'Kit nettoyage sonde radar',
     360, 120, 'VALIDEE', '22222222-2222-2222-2222-222222222222', '2025-12-03 07:15:00', 'Priorité sécurité traitée'),

    ('a4444444-4444-4444-4444-444444444444', 'f4444444-4444-4444-4444-444444444444',
     '33333333-3333-3333-3333-333333333333',
     'Analyse vibrations moteur convoyeur A',
     'Vibrations 7 mm/s axe vertical, bruit métallique intermittent',
     'Désalignement courroie après changement poulie',
     'Analyse en cours — mesures avant/après tension courroie',
     'Réglage tension courroie, alignement laser prévu',
     NULL,
     60, NULL, 'SOUMISE', NULL, NULL, NULL),

    ('a5555555-5555-5555-5555-555555555555', 'f5555555-5555-5555-5555-555555555555',
     '33333333-3333-3333-3333-333333333333',
     'Brouillon communication Modbus variateur',
     'Timeouts Modbus aléatoires toutes les 2h',
     NULL,
     NULL,
     NULL,
     NULL,
     NULL, NULL, 'BROUILLON', NULL, NULL, NULL),

    ('a6666666-6666-6666-6666-666666666666', 'f2222222-2222-2222-2222-222222222222',
     '33333333-3333-3333-3333-333333333333',
     'Tentative correction sans preuve — rejetée',
     'Alarme sporadique sans reproduction',
     'Cause supposée sans mesure',
     'Diagnostic incomplet',
     'Redémarrage simple',
     NULL,
     30, 15, 'REJETEE', '22222222-2222-2222-2222-222222222222', '2025-11-20 11:00:00', 'Analyse insuffisante, refaire avec mesures'),

    ('a7777777-7777-7777-7777-777777777777', 'f6666666-6666-6666-6666-666666666666',
     '33333333-3333-3333-3333-333333333333',
     'Réparation fuite joint pompe KSB',
     'Flaque huile sous palier arrière, débit réduit de 30 %',
     'Usure joint mécanique arbre par cavitation répétée',
     'Inspection palier : traces de cavitation sur roue. Joint mécanique HS.',
     'Remplacement joint mécanique, contrôle alignement, purge circuit',
     'Joint mécanique KSB type MG1-45',
     120, 90, 'VALIDEE', '22222222-2222-2222-2222-222222222222', '2026-02-11 10:00:00', NULL)
ON CONFLICT (id) DO NOTHING;

UPDATE failures
SET declarant_id = '11111111-1111-1111-1111-111111111111'
WHERE id IN (
    'f1111111-1111-1111-1111-111111111111',
    'f2222222-2222-2222-2222-222222222222',
    'f3333333-3333-3333-3333-333333333333',
    'f4444444-4444-4444-4444-444444444444',
    'f5555555-5555-5555-5555-555555555555',
    'f6666666-6666-6666-6666-666666666666'
)
AND declarant_id IS NULL;
