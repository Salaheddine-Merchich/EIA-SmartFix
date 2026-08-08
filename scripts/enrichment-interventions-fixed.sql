-- Script d'enrichissement interventions pour le RAG EIA SmartFix (VERSION CORRIGEE)
-- Crée 18 interventions VALIDEE avec champs RAG riches et formulations diversifiées

-- Interventions Instrumentation (5 interventions)
INSERT INTO interventions (id, failure_id, technicien_id, description, symptomes, cause_racine, analyse_technique, actions_correctives, pieces_remplacees, duree_arret_minutes, temps_intervention_minutes, statut_validation, validateur_id, date_validation, commentaire_validation)
VALUES
    -- PTI-056 Signal erratique
    ('11010101-1010-1010-1010-101010101010', 'f1010101-1010-1010-1010-101010101010', 
     (SELECT id FROM users WHERE email = 'mehdi@ocp.ma'), 
     'Réparation capteur pression hydraulique PTI-056 signal instable',
     'Signal pression oscillant rapidement 0-250 bars, alarme process, impossible régulation circuit',
     'Connexion électrique corrodée + blindage câble endommagé par vibrations',
     'Test isolation : 2.1 MΩ (spec >5 MΩ). Oxydation visible bornier IP67. Continuité blindage interrompue.',
     'Nettoyage contacts, graisse diélectrique, remplacement câble blindé 4x0.75mm² sur 15m',
     'Câble blindé Lapp ÖLFLEX 150 CY 4G0.75, bornier Wago 2273-203',
     45, 90, 'VALIDEE', 
     (SELECT id FROM users WHERE email = 'responsable@ocp.ma'), 
     '2026-07-15 16:00:00', 
     'Intervention bien menée, prévoir maintenance préventive câblage semestrielle'
    ),

    -- PTI-056 Dérive étalonnage  
    ('12010102-1010-1010-1010-101010101010', 'f1010102-1010-1010-1010-101010101010',
     (SELECT id FROM users WHERE email = 'mohamad@ocp.ma'),
     'Étalonnage capteur pression PTI-056 après dérive constatée',
     'Mesure décalée +15% vs manomètre étalon, pression réelle 200 bars affiché 230 bars',
     'Dérive naturelle cellule piézorésistive, température ambiante élevée +45°C',
     'Comparaison étalon classe 0.1% : écart linéaire +15% sur toute échelle. Capteur Honeywell STG94L.',
     'Recalibrage 2 points (0 et 250 bars), ajustement offset et gain via configurateur HART',
     'Aucune pièce, uniquement configuration software',
     30, 45, 'VALIDEE',
     (SELECT id FROM users WHERE email = 'kamal@ocp.ma'),
     '2026-07-20 17:30:00',
     'Étalonnage conforme, certificat métrologie émis'
    ),

    -- FIT-078 Aucun signal
    ('13020201-2020-2020-2020-202020202020', 'f2020201-2020-2020-2020-202020202020',
     (SELECT id FROM users WHERE email = 'ahmed@ocp.ma'),
     'Remplacement carte électronique débitmètre FIT-078',
     'Affichage --- sur écran local et superviseur, LED diagnostic rouge clignotante',
     'Carte électronique défaillante suite surtension lors orage, fusible grillé',
     'Contrôle alimentation : 0V au lieu 24VDC. Fusible F1 (2A) grillé. Test carte : aucun signal CPU.',
     'Remplacement carte électronique complète, reconfiguration paramètres process',
     'Carte Endress+Hauser ref 52018141, fusible Littelfuse 0218002.MXP',  
     120, 150, 'VALIDEE',
     (SELECT id FROM users WHERE email = 'responsable@ocp.ma'),
     '2026-07-18 18:15:00',
     'Installer parafoudre sur armoire pour éviter récidive'
    ),

    -- FIT-078 Mesure bloquée 0
    ('14020202-2020-2020-2020-202020202020', 'f2020202-2020-2020-2020-202020202020',
     (SELECT id FROM users WHERE email = 'technicien@ocp.ma'),
     'Nettoyage électrodes débitmètre électromagnétique FIT-078',
     'Débit affiché constant 0 m3/h, circulation visible dans conduite DN150, voyants OK',
     'Encrassement électrodes par dépôts calcaires, conductivité insuffisante',
     'Inspection visuelle : dépôt blanc cristallin sur électrodes. Mesure résistance >10 MΩ (normal <1 kΩ).',
     'Démontage brides, nettoyage acide chlorhydrique dilué 5%, rinçage, remontage joints neufs',
     'Joints EPDM DN150 PN16 Klinger, acide HCl technique 37%',
     60, 120, 'VALIDEE',
     (SELECT id FROM users WHERE email = 'kamal@ocp.ma'),
     '2026-07-25 19:45:00',
     'Prévoir nettoyage préventif trimestriel'
    ),

    -- AIT-134 Lecture instable pH  
    ('15030301-3030-3030-3030-303030303030', 'f3030301-3030-3030-3030-303030303030',
     (SELECT id FROM users WHERE email = 'mehdi@ocp.ma'),
     'Remplacement sonde pH analyseur AIT-134 instabilité mesure',
     'pH fluctuant 6.8 à 8.2 en continu, impossible régulation traitement, alarmes répétées',
     'Vieillissement électrode pH, dérive jonction liquide, bulles air',
     'Test sonde étalon pH7 : réponse lente 45s (spec <30s). Pente -52.1 mV/pH (spec -59.16). Age 18 mois.',
     'Remplacement sonde pH + référence, étalonnage 3 points (pH4/7/10), purge ligne',
     'Sonde Yokogawa PH450G-15-S-N-D avec câble 15m',
     40, 75, 'VALIDEE',
     (SELECT id FROM users WHERE email = 'responsable@ocp.ma'),
     '2026-07-22 14:20:00',
     'Sonde fonctionnelle, prévoir remplacement annuel'
    );

-- Interventions Automatisme (5 interventions)
INSERT INTO interventions (id, failure_id, technicien_id, description, symptomes, cause_racine, analyse_technique, actions_correctives, pieces_remplacees, duree_arret_minutes, temps_intervention_minutes, statut_validation, validateur_id, date_validation, commentaire_validation)  
VALUES
    -- PLC-067 Perte communication Ethernet/IP
    ('16040401-4040-4040-4040-404040404040', 'f4040401-4040-4040-4040-404040404040',
     (SELECT id FROM users WHERE email = 'mohamad@ocp.ma'),
     'Réparation communication Ethernet/IP automate PLC-067',
     'Perte liaison superviseur, voyant NET rouge, timeout MSG instructions, arrêt ligne production',
     'Câble Ethernet endommagé dans chemin de câbles, connecteur RJ45 oxydé',
     'Test continuité : paires 1-2 et 3-6 coupées. Connecteur vert-de-gris. RSTP non convergent.',
     'Tirage nouveau câble Cat6 blindé, sertissage connecteurs industriels IP67',
     'Câble Belden 7965E Cat6 FTP 50m, connecteurs Harting RJ45 IP67',
     180, 240, 'VALIDEE', 
     (SELECT id FROM users WHERE email = 'kamal@ocp.ma'),
     '2026-07-16 20:30:00',
     'Communication stable, documenter nouveau chemin câble'
    ),

    -- PLC-067 Module E/S défaillant
    ('17040402-4040-4040-4040-404040404040', 'f4040402-4040-4040-4040-404040404040',
     (SELECT id FROM users WHERE email = 'ahmed@ocp.ma'),
     'Remplacement module analogique 1756-IF8 automate Allen-Bradley',
     'Voies ANA0 à ANA3 valeurs aberrantes, 4-20mA lu comme 22.5mA, étalonnage impossible',
     'Module analogique HS, composant CAN défaillant voie 0 à 3',
     'Diagnostic RSLogix : CHANNEL_FAULT voies 0-3. Test boucle 4-20mA : 20mA lu 22.47mA. Module ref 1756-IF8/A.',
     'Remplacement module 1756-IF8, reconfiguration RSLogix5000, test toutes voies',
     'Module Allen-Bradley 1756-IF8/A 8 voies analogiques 4-20mA',
     90, 180, 'VALIDEE',
     (SELECT id FROM users WHERE email = 'responsable@ocp.ma'),  
     '2026-07-28 16:45:00',
     'Module fonctionnel, sauvegarder configuration'
    ),

    -- DI-089 Timeout Modbus TCP
    ('18050501-5050-5050-5050-505050505050', 'f5050501-5050-5050-5050-505050505050',
     (SELECT id FROM users WHERE email = 'technicien@ocp.ma'),
     'Configuration réseau module Modbus TCP DI-089 timeouts',
     'Communication sporadique Modbus, timeout lecture registres 40001-40016, supervision partielle',  
     'Congestion réseau Ethernet, switch port duplex mismatch, MTU inadapté',
     'Wireshark : paquets Modbus fragmentés. Switch port auto/100 vs module 100/full. RTT >500ms.',
     'Reconfiguration switch port full-duplex, réduction timeout Modbus 1000ms, VLAN dédiée',
     'Configuration software uniquement',
     60, 90, 'VALIDEE',
     (SELECT id FROM users WHERE email = 'kamal@ocp.ma'),
     '2026-07-19 18:20:00', 
     'Communication fluide, surveiller trafic réseau'
    ),

    -- UPS-012 Batterie défaillante
    ('19060601-6060-6060-6060-606060606060', 'f6060601-6060-6060-6060-606060606060',
     (SELECT id FROM users WHERE email = 'mehdi@ocp.ma'),
     'Remplacement batterie onduleur UPS-012 perte autonomie',
     'Alarme BATTERY FAULT, autonomie chutée à 3 minutes au lieu 15 minutes nominales',
     'Vieillissement batteries plomb-gel, sulfatation plaques, capacité résiduelle <40%',
     'Test décharge contrôlée : 3.2min à 80% charge nominale. Tension cellule <10.5V. Age 4 ans.',
     'Remplacement jeu 20 batteries plomb-gel, test autonomie, calibrage onduleur',
     '20x batterie APC RBC140 12V 9Ah plomb-gel étanche',
     120, 180, 'VALIDEE',
     (SELECT id FROM users WHERE email = 'responsable@ocp.ma'),
     '2026-07-21 15:40:00',
     'Autonomie restaurée 16 minutes, test mensuel programmé'
    ),

    -- UPS-012 Surtension réseau
    ('1a060602-6060-6060-6060-606060606060', 'f6060602-6060-6060-6060-606060606060',
     (SELECT id FROM users WHERE email = 'mohamad@ocp.ma'),
     'Réglage seuils protection onduleur UPS-012 surtensions',
     'Passage mode bypass fréquent, surtensions réseau 253V détectées, charge non protégée',
     'Seuils protection trop stricts, réseau instable, harmoniques perturbateurs',
     'Mesure réseau : 248-253V RMS, THD 8.2%. Seuils UPS : min 220V max 250V (trop strict).',
     'Reconfiguration seuils : 200-260V, activation écrêtage harmoniques, filtre actif',
     'Configuration software, mise à jour firmware v2.34',
     30, 60, 'VALIDEE', 
     (SELECT id FROM users WHERE email = 'kamal@ocp.ma'),
     '2026-07-30 16:15:00',
     'Onduleur stable, moins de basculements bypass'
    );

-- Interventions Mécanique (4 interventions)
INSERT INTO interventions (id, failure_id, technicien_id, description, symptomes, cause_racine, analyse_technique, actions_correctives, pieces_remplacees, duree_arret_minutes, temps_intervention_minutes, statut_validation, validateur_id, date_validation, commentaire_validation)
VALUES
    -- RED-045 Vibrations réducteur
    ('1b070701-7070-7070-7070-707070707070', 'f7070701-7070-7070-7070-707070707070',
     (SELECT id FROM users WHERE email = 'ahmed@ocp.ma'),
     'Réparation réducteur RED-045 vibrations et surchauffe',
     'Vibrations fortes >15mm/s RMS, bruit métallique, huile chaude +85°C, paliers usés',
     'Usure roulements entrée rapide, désalignement, huile contaminée particules métalliques',
     'Analyse vibratoire : harmoniques 14.2Hz (freq engrenage). Huile : particules fer 180ppm (limite 50ppm).',
     'Remplacement roulements à rouleaux coniques, alignement laser, vidange complète',
     'Roulement SKF 32018X entrée + SKF 32012X sortie, huile Shell Omala S4 WE 320',
     240, 360, 'VALIDEE',
     (SELECT id FROM users WHERE email = 'responsable@ocp.ma'),
     '2026-07-17 22:30:00',
     'Vibrations normalisées <2mm/s, surveillance continue'
    ),

    -- RED-045 Fuite huile  
    ('1c070702-7070-7070-7070-707070707070', 'f7070702-7070-7070-7070-707070707070',
     (SELECT id FROM users WHERE email = 'technicien@ocp.ma'),
     'Réparation fuite huile réducteur RED-045 niveau critique',
     'Fuite importante huile carter, niveau bas critique, témoin rouge allumé',
     'Joint spi arbre sortie durci fissuré, pression interne excessive, reniflard colmaté',
     'Joint spi 85x110x12 fissuré côté lèvre. Reniflard bouché graisse durcie. Pression +0.8 bar.',
     'Remplacement joint spi arbre sortie, nettoyage reniflard, appoint huile, test étanchéité',
     'Joint spi SKF 85x110x12 HMSA10 RG, huile Shell Omala S4 WE 320',
     180, 210, 'VALIDEE',
     (SELECT id FROM users WHERE email = 'kamal@ocp.ma'),  
     '2026-08-01 18:00:00',
     'Etanchéité restaurée, contrôle hebdomadaire niveau'
    ),

    -- VEN-123 Débit insuffisant
    ('1d080801-8080-8080-8080-808080808080', 'f8080801-8080-8080-8080-808080808080',
     (SELECT id FROM users WHERE email = 'mehdi@ocp.ma'),
     'Nettoyage ventilateur VEN-123 performance dégradée',
     'Débit mesuré 18 m³/min au lieu 30 m³/min nominal, dépression insuffisante atelier',
     'Encrassement pales et volute par poussières, filtre admission colmaté',
     'Epaisseur dépôt 5mm sur pales. Filtre G4 saturé >250 Pa. Vitesse rotation normale 1450 rpm.',
     'Démontage volute, nettoyage haute pression pales et diffuseur, remplacement filtre',
     'Filtre à air G4 490x592x48mm classe ISO ePM10 65%',
     120, 180, 'VALIDEE',
     (SELECT id FROM users WHERE email = 'responsable@ocp.ma'),
     '2026-07-23 17:45:00', 
     'Débit restauré 29.5 m³/min, nettoyage trimestriel'
    ),

    -- VEN-123 Courroie détendue
    ('1e080802-8080-8080-8080-808080808080', 'f8080802-8080-8080-8080-808080808080',
     (SELECT id FROM users WHERE email = 'mohamad@ocp.ma'),
     'Réglage tension courroie ventilateur VEN-123 glissement',
     'Courroie glisse sous charge, vitesse chute à 1200 rpm, bruit sifflement aigu',
     'Détente naturelle courroie trapézoïdale, tension insuffisante, usure gorges poulie',
     'Tension mesurée 95N (spec 150-180N). Usure gorge poulie moteur profondeur +2mm. Courroie SPZ 1250.',
     'Retension courroie via vis de réglage, contrôle parallélisme poulies, graissage paliers',
     'Graisse paliers SKF LGEP2 haute température',
     45, 75, 'VALIDEE', 
     (SELECT id FROM users WHERE email = 'kamal@ocp.ma'),
     '2026-08-03 19:30:00',
     'Courroie bien tendue, prévoir remplacement poulie usée'
    );

-- Interventions Électricité (4 interventions)
INSERT INTO interventions (id, failure_id, technicien_id, description, symptomes, cause_racine, analyse_technique, actions_correctives, pieces_remplacees, duree_arret_minutes, temps_intervention_minutes, statut_validation, validateur_id, date_validation, commentaire_validation)
VALUES  
    -- CTR-234 Contacteur ne ferme pas
    ('1f090901-9090-9090-9090-909090909090', 'f9090901-9090-9090-9090-909090909090',
     (SELECT id FROM users WHERE email = 'ahmed@ocp.ma'),
     'Remplacement contacteur CTR-234 défaillant pompe haute pression', 
     'Contacteur ne ferme plus, voyant défaut allumé, moteur pompe 75kW ne démarre pas',
     'Bobine contacteur grillée suite surtension, contacts principaux soudés par surintensité',
     'Mesure bobine : résistance infinie (normal 180Ω). Contacts principaux collés. Traces arc électrique.',
     'Remplacement contacteur complet, vérification protection surintensité, test manœuvres',
     'Contacteur Schneider LC1D95M7 bobine 220V 95A + bloc auxiliaire LAD4TBOU', 
     60, 120, 'VALIDEE',
     (SELECT id FROM users WHERE email = 'responsable@ocp.ma'),
     '2026-07-24 16:20:00',
     'Contacteur fonctionnel, vérifier réglage relais thermique'
    ),

    -- CTR-234 Contacts grillés  
    ('20090902-9090-9090-9090-909090909090', 'f9090902-9090-9090-9090-909090909090',
     (SELECT id FROM users WHERE email = 'technicien@ocp.ma'),
     'Remplacement jeu contacts contacteur CTR-234 arc électrique',
     'Arc électrique lors fermeture/ouverture, contacts noircis, résistance élevée',  
     'Usure contacts principaux par coupure charges inductives répétées sans protection arc',
     'Contacts piqués profondeur 3mm, résistance contact 15mΩ (spec <1mΩ). 850k manœuvres effectuées.',
     'Remplacement jeu contacts fixes et mobiles, nettoyage chambre coupure, réglage pression',
     'Kit contacts Schneider LX1D7M7 pour LC1D95, spray nettoyant contacts',
     30, 90, 'VALIDEE',
     (SELECT id FROM users WHERE email = 'kamal@ocp.ma'),  
     '2026-08-02 17:40:00',
     'Contacts neufs, installer limiteur surtension bobine'
    ),

    -- TRF-567 Surchauffe transformateur
    ('210a0a01-a0a0-a0a0-a0a0-a0a0a0a0a0a0', 'fa0a0a01-a0a0-a0a0-a0a0-a0a0a0a0a0a0', 
     (SELECT id FROM users WHERE email = 'mehdi@ocp.ma'),
     'Maintenance transformateur TRF-567 surchauffe enroulements',
     'Température enroulements +95°C, seuil alarme 85°C, ventilation forcée saturée poussière',
     'Encrassement ailettes refroidissement, ventilateurs HS, surcharge 110% nominale',
     'Epaisseur poussière 8mm sur ailettes. Ventilateur 1 bloqué. Charge mesurée 55kVA sur 50kVA nominal.',
     'Nettoyage ailettes air comprimé, remplacement ventilateur, délestage charge non critique',
     'Ventilateur axial 230V 180m³/h ref EBM W2E143-AA09-98', 
     90, 150, 'VALIDEE',
     (SELECT id FROM users WHERE email = 'responsable@ocp.ma'),
     '2026-07-26 18:15:00',
     'Température normalisée 68°C, surveiller charge'
    ),

    -- TRF-567 Harmoniques
    ('220a0a02-a0a0-a0a0-a0a0-a0a0a0a0a0a0', 'fa0a0a02-a0a0-a0a0-a0a0-a0a0a0a0a0a0',
     (SELECT id FROM users WHERE email = 'mohamad@ocp.ma'), 
     'Installation filtre harmoniques transformateur TRF-567 déformation onde',
     'THD tension >8%, déformation onde sinusoïdale, échauffement anormal neutre',
     'Charges non-linéaires (variateurs, éclairage LED), harmoniques rang 3 et 5 prédominants',  
     'Analyseur réseau : THD-U 9.2% (limite 5%). H3=4.8% H5=6.1%. Courant neutre 85A (charges 150A).',
     'Installation filtre passif LC accordé H3-H5, reconfiguration couplage triangle-étoile',
     'Filtre harmonique Schneider AccuSine PCS+ 50kVA, inductances 3x2.1mH',
     180, 240, 'VALIDEE', 
     (SELECT id FROM users WHERE email = 'kamal@ocp.ma'),
     '2026-08-04 20:45:00',
     'THD réduit à 3.2%, courant neutre normalisé'  
    );