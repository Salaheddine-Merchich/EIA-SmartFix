-- V7: Équipement demo famille Mécanique + panne associée

INSERT INTO equipment (id, code, designation, famille, zone, constructeur, mise_en_service)
VALUES
    ('dddddddd-dddd-dddd-dddd-dddddddddddd', 'POM-008', 'Pompe alimentation circuit mécanique', 'Mécanique', 'Zone D', 'KSB', '2021-06-01');

INSERT INTO failures (id, equipment_id, date_heure, criticite, zone_service, responsable_id, statut, description_initiale, code_defaut)
VALUES
    ('f6666666-6666-6666-6666-666666666666', 'dddddddd-dddd-dddd-dddd-dddddddddddd',
     '2026-02-10 09:30:00', 'HAUTE', 'Zone D', '22222222-2222-2222-2222-222222222222',
     'RESOLUE', 'Pompe mécanique fuite joint arbre, perte de débit', 'M003');

INSERT INTO interventions (id, failure_id, technicien_id, description, symptomes, cause_racine,
                           analyse_technique, actions_correctives, pieces_remplacees,
                           duree_arret_minutes, temps_intervention_minutes, statut_validation,
                           validateur_id, date_validation, commentaire_validation)
VALUES
    ('a7777777-7777-7777-7777-777777777777', 'f6666666-6666-6666-6666-666666666666',
     '33333333-3333-3333-3333-333333333333',
     'Réparation fuite joint pompe KSB',
     'Flaque huile sous palier arrière, débit réduit de 30 %',
     'Usure joint mécanique arbre par cavitation répétée',
     'Inspection palier : traces de cavitation sur roue. Joint mécanique HS.',
     'Remplacement joint mécanique, contrôle alignement, purge circuit',
     'Joint mécanique KSB type MG1-45',
     120, 90, 'VALIDEE', '22222222-2222-2222-2222-222222222222', '2026-02-11 10:00:00', NULL);
