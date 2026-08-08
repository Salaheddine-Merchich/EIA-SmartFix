-- V13: Populate knowledge_documents with real technical documentation
-- This provides genuine industrial knowledge to enhance RAG responses

-- Insert documents one by one for better error handling

INSERT INTO knowledge_documents (title, content, document_type, equipment_family, source) 
VALUES ('Guide diagnostic systemes hydrauliques industriels', 
'DIAGNOSTIC HYDRAULIQUE - METHODOLOGIE GENERALE

1. SYMPTOMES FREQUENTS ET CAUSES PROBABLES

Perte de pression :
- Fuite externe : joints, raccords, flexibles
- Fuite interne : joints de verin, clapet anti-retour
- Pompe defaillante : usure, cavitation
- Filtre colmate : verifier deltaP manometre

Mouvement lent ou saccade :
- Air dans le circuit : purger, verifier aspiration pompe
- Viscosite huile inadequate : temperature, grade
- Limitation de debit : reglage limiteur, dimensionnement

Surchauffe du circuit :
- Surcharge : verifier reglage limiteur de pression
- Refroidissement insuffisant : echangeur, ventilation
- Qualite huile : viscosite, pollution

2. MESURES DE DIAGNOSTIC

Pression : manometre aux points critiques
Temperature : 60-80°C normal, >90°C critique
Debit : debitmetre ou methode volumetrique
Analyse huile : particules, eau, acidite

3. STANDARDS DE REFERENCE

ISO 4413 : Transmissions hydrauliques - Regles generales
ISO 11171 : Essais de filtres hydrauliques
NAS 1638 : Classes de proprete des fluides', 
'guide', 'Hydraulique', 'Guide technique industriel');

INSERT INTO knowledge_documents (title, content, document_type, equipment_family, source) 
VALUES ('Procedures diagnostic moteurs electriques industriels', 
'DIAGNOSTIC MOTEURS ELECTRIQUES - GUIDE PRATIQUE

1. TESTS PRELIMINAIRES

Controle visuel :
- Etat des bornes, cables, contacteur
- Ventilation libre, pas d echauffement anormal
- Bruit anormal, vibrations

Tests electriques de base :
- Continuite des enroulements (multimetre)
- Isolement stator/masse : >1MOhm a 500V
- Equilibrage des phases : <5% difference resistance

2. DEFAUTS COURANTS

Moteur ne demarre pas :
- Verifier tension alimentation (±10% nominal)
- Controler contacteur, fusibles, relais thermique
- Mesurer resistance enroulements

Demarrages difficiles :
- Tension insuffisante en demarrage
- Surcharge mecanique excessive
- Rotor defaillant (bagues, cage)

Echauffement anormal :
- Surcharge : mesurer courant nominal
- Desequilibre phases : verifier reseau
- Defaut roulement : analyse vibratoire

3. MESURES DE PROTECTION

Relais thermique : reglage a 1,15 × In
Controle isolement periodique
Maintenance preventive : graissage, nettoyage', 
'procedure', 'Moteurs', 'Guide maintenance electrique');

INSERT INTO knowledge_documents (title, content, document_type, equipment_family, source) 
VALUES ('Troubleshooting variateurs de frequence industriels', 
'VARIATEURS DE FREQUENCE - DIAGNOSTIC ET DEPANNAGE

1. CODES DEFAUTS FREQUENTS

Defauts tension :
- Surtension bus DC : freinage excessif, coupure reseau
- Sous-tension : chute reseau, fusibles
- Desequilibre phases : mesurer tensions R,S,T

Defauts moteur :
- Surintensité : surcharge, court-circuit, parametrage
- Perte phase moteur : controler cablage U,V,W
- Defaut terre : isolement moteur/cables

Defauts communication :
- Timeout liaison serie : cablage, terminaison
- Profibus/Profinet : diagnostic LEDs, adresse IP
- Modbus RTU : polarite, baud rate, parite

2. PARAMETRAGE DE BASE

Parametres moteur essentiels :
- Tension nominale (V)
- Courant nominal (A) 
- Frequence nominale (50/60Hz)
- Vitesse nominale (tr/min)

Temps d acceleration/deceleration :
- Rampe trop courte : surintensité demarrage
- Rampe trop longue : productivite reduite

3. MAINTENANCE PREVENTIVE

Controle environnement :
- Temperature <40°C, ventilation libre
- Proprete : pas de poussiere conductrice
- Vibrations : fixation correcte

Verifications electriques :
- Serrage connexions : couple specifie
- Isolement cables moteur >1MOhm
- Controle ventilateur interne', 
'troubleshooting', 'Automatisme', 'Manuel variateurs industriels');

INSERT INTO knowledge_documents (title, content, document_type, equipment_family, source) 
VALUES ('FAQ diagnostic pannes industrielles courantes', 
'QUESTIONS FREQUENTES - DIAGNOSTIC INDUSTRIEL

Q: Comment identifier une panne electrique vs mecanique ?
R: Tests electriques d abord (tension, continuite, isolement), puis mecaniques (jeu, alignement, lubrification). Une panne electrique affecte generalement plusieurs fonctions, une panne mecanique est souvent localisee.

Q: Quelle priorite pour les mesures de diagnostic ?
R: 1) Securite (consignation, EPI), 2) Symptomes (observation, temoins), 3) Mesures simples (multimetre, thermometre), 4) Analyses approfondies (vibration, huile).

Q: Comment documenter une intervention ?
R: Symptomes observes, tests effectues, cause identifiee, actions correctives, pieces remplacees, duree intervention, preconisations preventives.

Q: Quand faire appel au constructeur ?
R: Defaut sous garantie, modification parametrage complexe, panne recurrente non resolue, mise a jour firmware, formation technique.

Q: Comment optimiser les stocks de pieces ?
R: Analyse ABC (rotation), pieces critiques (delai approvisionnement long), redondance active/passive, mutualisation entre equipements similaires.

Q: Criteres de remplacement vs reparation ?
R: Cout reparation > 60% cout neuf, obsolescence fournisseur, indisponibilite pieces, fiabilite degradee, evolution technologique.', 
'faq', NULL, 'Base de connaissances maintenance industrielle');