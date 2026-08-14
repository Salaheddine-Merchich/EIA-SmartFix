-- V24: Manufacturer PDF knowledge documents for RAG enrichment
-- Complements generic OCP guides from V13/V17

INSERT INTO knowledge_documents (title, content, document_type, equipment_family, source)
VALUES (
    'Manuel ABB ACS880 Spinning — Fault tracing',
    'ABB ACS880 SPINNING TRAVERSE +N5500 — DIAGNOSTIC DEFAUTS

Source: ACS880 Spinning FW manual, Fault tracing p.553+

CODES DEFAUT BLOQUANTS (exemples):
2310 Overcurrent: verifier charge moteur, rampes grp 23/26/28, cable moteur, param grp 99, encodeur.
2340 Short circuit: cablage moteur, param 99.10, pas de condensateurs sur cable, reboot 96.08.
2330 Earth leakage: mesurer isolation moteur/cable, retirer condensateurs, scalar 99.04.
3381 Output phase loss: connecter 3 phases moteur.
4210 IGBT overtemperature: ambiance, ventilation, radiateur, puissance vs moteur.
3210 DC overvoltage: param 30.30, alimentation, chopper freinage, rampe DEC.
3220 DC undervoltage: cablage, fusibles, redresseur, phase reseau.
3130 Input phase loss: fusibles, connexions, desequilibre reseau.
3385 Autophasing failed: param 21.13, impulsion Z encodeur, ID run, 99.03.

ALARMES (exemples):
A581 Fan: param 95.20, remplacer ventilateur.
A780 Motor stall: charge, param 31.24.
A798 Encoder comm loss: module FEN, param 90.41/90.51, cable encodeur.
A5A0 STO: circuit securite, param 31.22.
AFEB Run enable: param 20.12, fieldbus.

CODES FILATURE:
D200 Section sum not 100%: ajuster pattern grp 75-78 Length total 100%.
D201 Doff end: param 81.06 temps ou 81.07 longueur doff.

TRAVERSE: 2310 overcurrent traverse — rampes 88.xx, charge mecanique traverse.',
    'manual',
    'Variateur',
    'ACS880 Spinning FW manual +N5500'
) ON CONFLICT (title, source) DO NOTHING;

INSERT INTO knowledge_documents (title, content, document_type, equipment_family, source)
VALUES (
    'Manuel Hitachi SJ200 — Depannage ch.6',
    'HITACHI VARIATEUR SJ200 — DEPANNAGE ET MAINTENANCE

Source: Manuel utilisation SJ200 NB650XA, chapitre 6

CODES ERREUR:
E01 Surintensite vitesse constante: CC sortie, moteur grippé, bi-tension incorrecte.
E02 Surintensite deceleration: recuperation energetique excessive.
E03 Surintensite acceleration: rampe ACC trop rapide.
E05 Surcharge moteur: protection thermique electronique.
E07 Surtension bus CC: freinage recuperatif.
E08 EEPROM: ne pas couper alim avec entree RS active; verifier parametres.
E09 Sous-tension: alimentation entree faible.
E14 Defaut terre: isolement moteur/cable.
E21 Thermique variateur: nettoyer radiateur, -10 a 40C ambiant.
E35 Surchauffe moteur: sonde entrees 6/L.

JOURNAL DEFAUTS: D081 defaut courant, D082/D083 historique.
Effacement: touche STOP/RESET ou B084=00 pour journal seul.

SYMPTOMES SANS CODE:
Moteur ne demarre pas: Run, F001>0, FW/RV, RS/FRS.
Sens rotation inverse: permuter U-V-W ou F004.
Vitesse n atteint pas consigne: entree analogique, A004, A061.
Parametres non sauvegardes: touche Store, attendre 6s avant remise sous tension.',
    'manual',
    'Variateur',
    'Hitachi SJ200 NB650XA'
) ON CONFLICT (title, source) DO NOTHING;

INSERT INTO knowledge_documents (title, content, document_type, equipment_family, source)
VALUES (
    'Manuel VEICHI SI23 — Alarmes et protections pompage solaire',
    'VEICHI SI23-D5-004G — ALARMES ERREURS ET PROTECTIONS PV

Source: Manuel utilisation variateur pompage solaire VEICHI SI23

PARAMETRES CLES:
F00.02 commande: 0 console, 1 commutateur X1, 3 RS485.
F14.11 Go to sleep voltage, F14.12 Wake up voltage.
F14.14 frequence minimale, F14.17 courant marche a sec.
F14.20 surintensite, F14.23 puissance minimale.
Relais manque d eau: TA-TC TB-TC.

CODES ERREUR E:
E.LU2 sous-tension marche: tension entree, contacteur DC.
E.oU1-4 surtensions: reseau/PV, DEC, force externe.
E.oC1-3 surintensites: ACC/DEC, V/F, auto-reglage moteur.
E.oL1-2 surcharge moteur/variateur: couple, param moteur.
E.SC systeme anormal: CC sortie, CEM.
E.oH1-2 surchauffe: canal air, ventilateur.
E.ILF/E.oLF perte phase entree/sortie.
E.PAn defaut clavier RJ45.

ALARMES A:
A.LPn sommeil PV bas: F14.11/F14.12.
A.LFr basse frequence: F14.14/F14.16.
A.LuT marche a sec: F14.17-F14.19, niveau eau, relais TA-TC.
A.oLd surintensite: F14.20-F14.22.
A.LPr puissance min: F14.23/F14.25.

FAQ: debit faible = sens moteur; veille 0Hz = commande X1; courant DC = F14.30/F14.31.',
    'manual',
    'Variateur',
    'VEICHI SI23-D5-004G manual'
) ON CONFLICT (title, source) DO NOTHING;

INSERT INTO knowledge_documents (title, content, document_type, equipment_family, source)
VALUES (
    'Manuel Goodrive 100-PV — Diagnostic ch.7',
    'INVT GOODRIVE 100-PV — DIAGNOSTIC DEFAUTS ET SOLUTIONS

Source: Manuel pompage solaire Goodrive 100-PV, chapitre 7 p.64+

PROCEDURE APRES DEFAUT:
1. Verifier clavier. 2. Consulter P07 historique. 3. Tableau codes ci-dessous. 4. Eliminer defaut. 5. Reset VF.

CODES PUISSANCE:
OUt1/2/3 protection phases U/V/W: ACC, IGBT, CEM, cablage.
OV1/2/3 surtension ACC/DEC/constant: alimentation, freinage, energie recuperee.
OC1/2/3 surintensite: ACC/DEC, charge, CC/terre, VF sous-dimensionne.
UV sous-tension bus: alimentation/PV faible.
OL1/2 surcharge moteur/VF: courant nominal, ACC, charge.
SPI/SPO perte phase entree/sortie.

CODES SYSTEME:
OH1/OH2 surchauffe redresseur/onduleur: air, ventilateur.
CE communication: baud, adresse RS485.
EEP EEPROM: STOP/RST, carte controle.
ETH1/2 court-circuit terre sortie.
dEu deviation vitesse: charge bloquee.

CODES PV SPECIFIQUES:
PINV polarite PV inversee: permuter +/- .
PVOC surintensite PV: ACC/DCC, puissance VF.
PVOV surtension PV: reduire panneaux serie.
PVLv sous-tension PV: augmenter strings, plein soleil.
E-422 comm module boost: 4 fils RS422.
OV surtension bus boost: P19.07 P19.08.

ALARMES NIVEAU EAU:
A-LS faible lumiere, A-LL reservoir vide, A-tF plein eau, A-tL manque eau.
tSF sonde hydraulique endommagee: remplacer sonde.',
    'manual',
    'Variateur',
    'Goodrive 100-PV manual FR'
) ON CONFLICT (title, source) DO NOTHING;

INSERT INTO knowledge_documents (title, content, document_type, equipment_family, source)
VALUES (
    'Manuel ABB ACS880-11 — Maintenance installation',
    'ABB ACS880-11 — MAINTENANCE ET INSTALLATION

Source: FR_ACS880-11_HW_H-1 installation manual

MAINTENANCE PREVENTIVE:
Nettoyage exterieur variateur et radiateur periodique.
Remplacement ventilateurs refroidissement R3/R6/R8 selon intervalles ch 11.
Condensateurs: courbe duree de vie vs temperature ambiante; reactiver apres stockage.
Inspection annuelle recommandee apres mise en route.

MESURE ISOLEMENT (ch 6):
Mesurer isolement variateur, cable reseau, moteur et cablage avant MES.
Controle compatibilite systeme mise a la terre.
Blindage cable moteur cote moteur.

TAILLES ET CARACTERISTIQUES:
Gammes R3/R6/R8, unité ZCU-12, ventilateurs principal et auxiliaire.
Un=400V et 500V, courants I2/Ifs/Iint selon mode utilisation.
Mode grande vitesse param 95.15 des 120 Hz.

PAS DE TABLEAU CODES DEFAUT dans ce manuel installation — utiliser manuel firmware Spinning pour fault tracing.',
    'manual',
    'Variateur',
    'ABB ACS880-11 HW manual FR'
) ON CONFLICT (title, source) DO NOTHING;
