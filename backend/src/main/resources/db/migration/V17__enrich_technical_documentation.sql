-- V17: Enrichir la documentation technique avec des guides spécialisés par famille d'équipement
-- Améliore la couverture RAG avec des documents plus spécifiques aux équipements OCP

-- Documents spécialisés Hydraulique
INSERT INTO knowledge_documents (title, content, document_type, equipment_family, source) 
VALUES ('Guide pompes centrifuges OCP', 
'POMPES CENTRIFUGES - DIAGNOSTIC ET MAINTENANCE OCP

1. TYPES DE POMPES COURANTES OCP

Pompes process :
- Pompes centrifuges monocellulaires : débits 50-500 m³/h
- Pompes multicellulaires : pressions 10-60 bars
- Pompes chimiques : résistance acides phosphoriques
- Pompes dosage : précision ±2% pour additifs

2. PANNES FREQUENTES POMPES CENTRIFUGES

Cavitation :
- Symptômes : bruit, vibrations, chute débit
- Causes : NPSH insuffisant, aspiration bouchée
- Tests : manomètre aspiration, analyse vibratoire
- Solution : vérifier hauteur géométrique, nettoyer crépines

Usure impulseur :
- Symptômes : rendement diminué, surconsommation
- Diagnostic : courbe caractéristique Q-H
- Mesures : jeu impulseur-volute <0,5mm
- Remplacement si jeu >2mm

Désalignement :
- Symptômes : vibrations radiales >4mm/s
- Contrôle : comparateur au laser, méthode cadran
- Tolérance : <0,1mm parallélisme, <0,05mm concentricité

3. MAINTENANCE PREVENTIVE POMPES

Quotidien :
- Contrôle températures paliers <80°C
- Pression refoulement dans plage nominale
- Débit process selon consigne
- Absence fuites presse-étoupe

Hebdomadaire :
- Analyse vibratoire points de mesure
- Contrôle niveau huile réducteur
- Vérification étanchéité
- Test alarmes et sécurités

Mensuel :
- Contrôle alignement
- Mesure intensité moteur
- Contrôle serrage boulonnerie
- Analyse huile si circuit ferme', 
'guide', 'Hydraulique', 'Guide maintenance OCP');

INSERT INTO knowledge_documents (title, content, document_type, equipment_family, source) 
VALUES ('Diagnostic circuits hydrauliques haute pression', 
'CIRCUITS HYDRAULIQUES HAUTE PRESSION - DIAGNOSTIC AVANCE

1. CARACTERISTIQUES CIRCUITS HP OCP

Pressions service :
- Circuits standard : 150-200 bars
- Circuits spéciaux : 300-400 bars
- Circuits test : jusqu''à 500 bars
- Seuils alarme : 90% pression nominale

Fluides utilisés :
- HLP 46 : température -10/+80°C
- HLP 68 : charges lourdes >50°C
- Fluides biodégradables : zones sensibles
- Additifs anticorrosion phosphates

2. DIAGNOSTIC FUITES HAUTE PRESSION

Détection fuites externes :
- Inspection visuelle systématique
- Papier détecteur sous raccords
- Caméra thermique : échauffement localisé
- Produit traceur fluorescent UV

Fuites internes vérins :
- Test maintien pression : <2% en 5min
- Contrôle dérive position charge
- Mesure débit retour à vide
- Analyse huile : particules métalliques

Fuites distributeurs :
- Contrôle chute pression pilotage
- Test étanchéité tiroir : manomètre différentiel
- Vérification temps commutation
- Analyse pollution huile NAS 8 max

3. GESTION POLLUTION HYDRAULIQUE

Sources contamination :
- Apport externe : joints défaillants, bouchons
- Génération interne : usure, érosion, cavitation
- Dégradation fluide : oxydation, hydrolyse

Analyse pollution :
- Comptage particules : NAS 1638 classe 8 max
- Teneur eau : <0,1% en poids
- Indice acidité : <2mg KOH/g
- Point éclair : >200°C

Actions correctives :
- Filtration 10 microns absolu minimum
- Dégazage sous vide si >8% air dissous
- Echange huile si pollution >classe 10
- Modification circuit si génération excessive', 
'troubleshooting', 'Hydraulique', 'Procédures OCP haute pression');

INSERT INTO knowledge_documents (title, content, document_type, equipment_family, source) 
VALUES ('Maintenance preventive verins hydrauliques', 
'VERINS HYDRAULIQUES - MAINTENANCE SYSTEMATIQUE

1. TYPES VERINS OCP

Vérins process :
- Simple effet : gravité ou ressort rappel
- Double effet : course 50mm à 3000mm
- Télescopiques : gain place, course importante
- Rotatifs : rotation 90-360°, couple élevé

2. CONTROLES PREVENTIFS VERINS

Inspection visuelle :
- État tige : rayures, corrosion, usure
- Joints : fuites, craquements, durcissement
- Fixations : jeu, usure rotules
- Protection : soufflets, racleurs

Tests fonctionnels :
- Temps cycle : nominal ±10%
- Pression service : manomètre aux orifices
- Maintien position : dérive <2mm/5min
- Amortissement fin course : progressif

Mesures dimensionnelles :
- Diamètre tige : micromètre, tolérance h9
- Rectitude tige : comparateur 0,1mm/m max
- Jeu chemise : calibre télescopique
- Usure chrome : épaisseur ultrason >25µm

3. REPARATION VERINS

Démontage sécurisé :
- Décompression complète circuit
- Extraction tige : tire extracteur adapté
- Nettoyage : dégraissant, inspection UV
- Contrôle dimensionnel : gabarit usure

Remplacement joints :
- Joints tige : NBR 90 Shore A standard
- Joints piston : PTFE + élastomère
- Racleurs : polyuréthane double lèvre
- Graissage montage : graisse compatible

Tests après remontage :
- Pression d''épreuve : 1,5 × Pn mini 5min
- Étanchéité : fuite nulle 24h
- Fonctionnement : cycles lents puis rapides
- Réglage amortisseurs : vis + contre-écrou', 
'procedure', 'Hydraulique', 'Guide réparation OCP');

-- Documents spécialisés Moteurs
INSERT INTO knowledge_documents (title, content, document_type, equipment_family, source) 
VALUES ('Procedures moteurs asynchrones industriels', 
'MOTEURS ASYNCHRONES INDUSTRIELS - PROCEDURES OCP

1. GAMME MOTEURS OCP

Basse tension 400V :
- 0,75 à 200 kW standard process
- IP55 : environnement poussiéreux
- IC411 : ventilation forcée >100kW
- Classe F : isolation 155°C

Haute tension 6,6kV :
- 200 à 5000 kW gros équipements
- Protection IP54 minimum
- Sondes PT100 paliers obligatoires
- Démarreurs électroniques >500kW

2. PROCEDURES DEMARRAGE MOTEURS

Contrôles pré-démarrage :
- Isolement enroulements >1MOhm/kV + 1MOhm
- Continuité circuit protection thermique
- Niveau huile réducteur si couplé
- Accouplement libre rotation manuelle

Démarrage progressif :
- Moteurs <15kW : direct sur réseau
- 15-100kW : démarreur étoile-triangle
- >100kW : démarreur électronique progressive
- Rampe accélération : 10-30s selon inertie

Surveillance démarrage :
- Courant : <7×In pointe, <In régime
- Temps démarrage : <20s pour ventilateurs
- Vibrations : <2,8mm/s classe II
- Température paliers : <80°C en régime

3. DIAGNOSTIC DEFAUTS ROTORIQUES

Court-circuit barres rotor :
- Symptômes : courant déséquilibré, vibrations
- Test : courant statorique FFT 2×s×f ±2Hz
- Oscilloscope : modulation amplitude 2sf
- Gravité : >-45dB alerte, >-35dB défaut

Rotor déséquilibré :
- Fréquence défaut : 1×f réseau
- Amplitude vibration : >4,5mm/s alarme
- Phase : direction déséquilibre mesure 3 axes
- Correction : ajout masses ou usinage

Défaut roulements :
- BPFI = Nb×f×(1-d/D×cos α)/2
- BPFO = Nb×f×(1+d/D×cos α)/2  
- BSF = D×f×(1-(d/D×cos α)²)/(2d)
- FTF = f×(1-d/D×cos α)/2
- Détection : analyse enveloppe accélération', 
'procedure', 'Moteurs', 'Manuel diagnostic moteurs OCP');

INSERT INTO knowledge_documents (title, content, document_type, equipment_family, source) 
VALUES ('Diagnostic roulements et vibrations moteurs', 
'ROULEMENTS MOTEURS - ANALYSE VIBRATOIRE OCP

1. TYPES ROULEMENTS MOTEURS OCP

Roulements standards :
- Côté accouplement : à billes 6000 série
- Côté libre : à rouleaux NU série
- Étanchéité : 2RS contact, 2Z sans contact
- Graisse : lithium EP2, regraissage programmé

Roulements spéciaux :
- Moteurs verticaux : butée combinée
- Haute température : graisse haute température
- Environnement humide : étanchéité renforcée
- Vitesse élevée : cage plastique/bronze

2. FREQUENCES DEFAUTS ROULEMENTS

Calcul fréquences caractéristiques :
- f = fréquence rotation moteur Hz
- d = diamètre billes/rouleaux mm  
- D = diamètre primitif mm
- Nb = nombre éléments roulants
- α = angle contact (0° roulements rouleaux)

Fréquences surveillance :
- Bague externe BPFO : surveiller 1-10×BPFO
- Bague interne BPFI : surveiller 1-10×BPFI  
- Éléments roulants BSF : surveiller 1-5×BSF
- Cage FTF : surveiller 1×FTF ±10%

Harmoniques pathologiques :
- BPFO×2, BPFO×3 : dégradation bague externe
- Bandes latérales BPFI±1×f : bague interne
- BSF modulé FTF : défaut élément roulant
- Sous-harmoniques FTF/2 : cage défaillante

3. CRITERES EVALUATION ROULEMENTS

Niveaux d''alerte vibratoires :
- Vitesse 1-1000Hz : 2,8mm/s bon, 7,1mm/s alerte
- Accélération enveloppe : <0,5g bon, >2g défaut
- Pics résonance : <3g acceptable, >10g critique
- Facteur crête : <3 bon, >5 défaut développé

Analyse température :
- Échauffement normal : 40-60°C ambiant +40°C
- Alerte température : >80°C palier
- Gradient admissible : <2°C/min montée
- Défaut graissage : montée rapide >5°C/min

Actions correctives :
- Regraissage préventif : 2000h ou annuel
- Remplacement planifié : >7,1mm/s vitesse
- Surveillance renforcée : >2,8mm/s
- Arrêt immédiat : >28mm/s ou >100°C', 
'troubleshooting', 'Moteurs', 'Guide analyse vibratoire OCP');

-- Documents spécialisés Automatisme  
INSERT INTO knowledge_documents (title, content, document_type, equipment_family, source) 
VALUES ('Diagnostic automates Siemens S7 OCP', 
'AUTOMATES SIEMENS S7 - DIAGNOSTIC SPECIFIQUE OCP

1. GAMME AUTOMATES OCP

S7-1500 process :
- CPU 1516-3 PN/DP : 1MB mémoire travail
- Modules E/S ET200SP déportées
- Communication Profinet RT classe conformité C
- Redondance : CPU 1518-4 PN/DP mode H

S7-1200 auxiliaires :
- CPU 1214C : petites applications <200 E/S
- Communication Ethernet intégrée
- Modules d''extension signal 1200 série
- Écrans HMI KTP700 Basic couplage direct

2. DIAGNOSTIC DEFAUTS CPU S7

Codes défaut CPU fréquents :
- 16#8001 : défaut alimentation 24V capteurs
- 16#8010 : timeout communication partenaire
- 16#8020 : erreur configuration matérielle
- 16#8040 : cycle programme dépassé (>100ms)

États CPU diagnostiqués :
- STOP : LED rouge fixe, programme arrêté
- STARTUP : LED jaune clignotante, initialisation  
- RUN : LED verte fixe, fonctionnement normal
- ERROR : LED rouge clignotante, défaut bloquant

Diagnostic mémoire :
- Mémoire travail disponible >20% minimum
- Blocs données DB : occupation <80%
- Pile des blocs : débordement possible
- Temps cycle : <50ms recommandé process

3. DIAGNOSTIC COMMUNICATION PROFINET

États liaison Profinet :
- Link : LED verte, liaison physique OK
- Activity : LED jaune clignotante, trafic réseau
- Maintenance : LED orange, diagnostic préventif
- Error : LED rouge, défaut communication

Tests réseau Profinet :
- Ping esclaves : <2ms temps réponse
- Diagnostic topologie : Step7 vue réseau
- Analyse trafic : Wireshark capture Profinet
- Qualité signal : TDR réflectométrie

Paramètres optimisés OCP :
- Temps cycle IO : 1ms process critique
- Temps surveillance : 3×temps cycle mini
- Redondance media : 2 switchs managés
- VLAN séparation : process/supervision/maintenance

4. MAINTENANCE PREVENTIVE S7

Contrôles périodiques :
- Sauvegarde programme : hebdomadaire
- Diagnostic buffer CPU : mensuel  
- Test entrées/sorties : semestriel
- Mise à jour firmware : selon bulletin Siemens

Surveillance paramètres :
- Température CPU <60°C  
- Tension alimentation 24V ±5%
- Consommation E/S <budget alimentation
- Temps cycle programme <seuil configuré', 
'guide', 'Automatisme', 'Procédures S7 OCP');

INSERT INTO knowledge_documents (title, content, document_type, equipment_family, source) 
VALUES ('Guide communication Profinet industrielle', 
'PROFINET - GUIDE IMPLEMENTATION OCP

1. ARCHITECTURE PROFINET OCP

Topologie réseau :
- Backbone fibre optique : redondance en anneau
- Switchs industriels : gérés VLAN, QoS, RSTP
- Câblage cuivre : Cat5e minimum, longueur <100m
- Connexions M12 : IP67 environnement industriel

Classes conformité :
- Classe A : communication standard TCP/IP
- Classe B : temps réel RT cyclique <10ms
- Classe C : temps réel IRT synchrone <1ms
- Classe OCP : RT pour process, IRT sécurité

2. CONFIGURATION PROFINET

Adressage IP :
- Plage OCP : 192.168.10.0/24 process
- Plage supervision : 192.168.20.0/24
- Gateway : 192.168.10.1 vers réseau entreprise
- DNS : résolution noms équipements

Configuration temps réel :
- Send Clock : 1ms cycle de base
- Reduction Ratio : 1,2,4,8 selon criticité
- Update Time : temps actualisation E/S
- Watchdog Time : 3×Update Time minimum

Paramètres QoS :
- Priorité RT : DSCP 46 (EF)
- Trafic supervision : DSCP 34 (AF41)
- Maintenance : DSCP 10 (AF11)  
- Best effort : DSCP 0

3. DIAGNOSTIC PROFINET

Outils diagnostic intégrés :
- Step7 : vue topologique, état équipements
- Proneta : scan réseau, test performance
- Wireshark : analyse trames Profinet DCP/RT
- Primary Setup Tool : configuration initiale

Tests performance :
- Jitter cycle : <25µs classe RT
- Temps réponse ping : <2ms équipements
- Bande passante : <30% charge liens
- Perte trames : 0 en fonctionnement normal

Défauts fréquents :
- Duplex mismatch : auto-négociation forcée
- Boucle réseau : STP/RSTP mal configuré
- Adresse IP dupliquée : détection DCP
- Câblage défaillant : TDR, certification Cat5e

4. MAINTENANCE PROFINET

Surveillance proactive :
- SNMP monitoring switchs : charge CPU <80%
- Compteurs erreurs : CRC, collisions, jabber
- Température switchs : <50°C ambiant industriel
- Alimentation redondée : basculement automatique

Sauvegarde configuration :
- Fichiers HW Config : Step7 projet
- Configuration switchs : SNMP ou CLI
- Topologie réseau : documentation à jour
- Certificats sécurité : sauvegarde centralisée', 
'guide', 'Automatisme', 'Guide réseaux industriels OCP');

-- Documents spécialisés Convoyeurs (nouvelle famille)
INSERT INTO knowledge_documents (title, content, document_type, equipment_family, source) 
VALUES ('Maintenance bandes transporteuses OCP', 
'BANDES TRANSPORTEUSES - MAINTENANCE OCP

1. TYPES BANDES OCP

Bandes process phosphates :
- EP textile : 3-5 plis, épaisseur 8-12mm
- Résistance traction : 400-630 N/mm largeur
- Revêtement anti-abrasion : épaisseur 6+2mm
- Largeur standard : 800, 1000, 1200, 1400mm

Bandes spéciales :
- Résistantes huile : NBR revêtement
- Anti-feu : norme ISO340 classe 2B
- Alimentaire : FDA, contact accidentel
- Haute température : jusqu''à 200°C

2. CONTROLES BANDES TRANSPORTEUSES

Inspection visuelle quotidienne :
- Usure revêtement : <2mm épaisseur mini
- Dechirures longitudinales : >100mm réparation
- Déchirures transversales : >50% largeur arrêt
- Bords effilochés : <20mm depuis limite toile

Contrôle tension bande :
- Tension statique : 1% allongement nominal
- Tension service : force tendeur hydraulique
- Mesure déflexion : règle 1m entre 2 rouleaux
- Déflexion admissible : 2-3% distance rouleaux

Alignement bande :
- Déport latéral : <5% largeur bande
- Contrôle tambours : faux rond <2mm
- Parallélisme tambours : <2mm sur largeur
- Réglage rouleaux porteurs : ±2° maximum

3. MAINTENANCE PREVENTIVE CONVOYEURS

Graissage paliers :
- Roulements tambours : graisse EP2 lithium
- Fréquence : 500h fonctionnement ou 3 mois
- Quantité : calcul selon vitesse et charge
- Température : contrôle IR <80°C

Nettoyage système :
- Raclage primaire : lame polyuréthane
- Raclage secondaire : lame métallique
- Brosses rotatives : nettoyage retour bande
- Arrosage haute pression : éviter roulements

Contrôle motoréducteurs :
- Niveau huile : hebdomadaire, appoint SAE 220
- Analyse vibratoire : mensuelle, <4,5mm/s
- Température : <80°C carter sous charge
- Consommation électrique : ±10% nominal

4. REPARATIONS COURANTES BANDES

Réparation à chaud :
- Découpe zone endommagée : cutter thermique
- Préparation surface : ponçage grain 60-80
- Nettoyage : dégraissant, séchage complet
- Collage : colle polyuréthane bi-composant

Réparation à froid :
- Patches caoutchouc : <300×300mm
- Agrafes mécaniques : charges légères uniquement
- Vulcanisation presse : réparations définitives
- Contrôle adhérence : pelage 15N/mm mini

Jonction bande :
- Découpe biaise : angle 22° épaisseur bande
- Ponçage échelonné : dégradé 3-4 étapes
- Enduction colle : 2 couches, séchage
- Pressage : 0,7MPa, 24h à température ambiante', 
'procedure', 'Convoyeurs', 'Manuel maintenance convoyeurs OCP');

INSERT INTO knowledge_documents (title, content, document_type, equipment_family, source) 
VALUES ('Diagnostic reducteurs et motoréducteurs', 
'REDUCTEURS MOTORÉDUCTEURS - DIAGNOSTIC OCP

1. TYPES REDUCTEURS OCP

Réducteurs à engrenages :
- Trains cylindriques : rendement 98% par étage
- Trains coniques : changement direction 90°
- Trains épicycloïdaux : compacité, rapport élevé
- Huile service : ISO VG 220, changement 4000h

Motoréducteurs intégrés :
- Arbre creux : montage direct sur tambour
- Couples : 500 à 50000 Nm disponibles
- Rapports : 5:1 à 500:1 selon application
- Refroidissement : ventilation forcée si >15kW

2. DIAGNOSTIC VIBRATOIRE REDUCTEURS

Fréquences engrenages :
- Fengrenant = (Z1×n1)/60 = (Z2×n2)/60
- Z1, Z2 : nombres dents pignon, roue
- n1, n2 : vitesses rotation tr/min
- Harmoniques 2×, 3×, 4× fengrenant surveillées

Défauts engrenages courants :
- Écaillage : pics fengrenant +harmoniques
- Usure uniforme : montée niveau large bande
- Fissure dent : pics 1×frotation ±fengrenant
- Désalignement : pics 2× et 3×frotation

Analyse huile diagnostic :
- Particules Fe : <200ppm normal, >500ppm alerte
- Particules Cu : <50ppm normal, >100ppm alerte  
- Viscosité : ±10% spécification ISO VG
- Indice acidité : <2mg KOH/g, changement si >4

3. SURVEILLANCE THERMIQUE REDUCTEURS

Températures normales :
- Carter réducteur : ambiant +40°C maximum
- Huile service : <80°C mesure permanente
- Paliers roulements : <90°C IR ponctuel
- Joints d''étanchéité : <60°C préservation

Points contrôle température :
- Thermomètre cadran : carter supérieur
- Sondes PT100 : paliers critiques >50kW
- Contrôle IR : rondeau systématique mensuel
- Thermostat sécurité : arrêt >100°C

Échauffement anormal :
- Surcharge mécanique : couple >nominal
- Défaut graissage : niveau insuffisant
- Viscosité inadaptée : trop épaisse/fluide
- Défaut roulement : grippage, jeu excessif

4. MAINTENANCE REDUCTEURS

Vidange huile :
- Première : 500h rodage impératif
- Courante : 4000h ou 2 ans maximum
- Huile synthétique : 8000h possible
- Rinçage circuit : huile de lavage dédiée

Contrôle étanchéité :
- Joints à lèvres : graissage compatible
- Déflecteurs : centrifugation huile efficace
- Rondelles feutre : imprégnation maintenue
- Évents : perméabilité air, étanchéité huile

Remplacement roulements :
- Démontage : extracteur hydraulique
- Contrôle portées : rugosité, ovalisation
- Montage : chauffage 80-100°C uniforme
- Jeu fonctionnement : C2/C3 selon charge', 
'troubleshooting', 'Convoyeurs', 'Guide diagnostic réducteurs OCP');