#!/usr/bin/env python3
"""Generate V23__seed_pdf_failures_interventions.sql from manufacturer PDF data."""

from __future__ import annotations

import textwrap
from pathlib import Path

EQUIP = {
    "VAR-ACS-SPIN": "b0010001-0001-0001-0001-000000000001",
    "VAR-ACS-TRV": "b0010002-0002-0002-0002-000000000002",
    "MOT-FIL": "b0010003-0003-0003-0003-000000000003",
    "MOT-TRV": "b0010004-0004-0004-0004-000000000004",
    "ENC-FEN": "b0010005-0005-0005-0005-000000000005",
    "FREIN-MEC": "b0010006-0006-0006-0006-000000000006",
    "VAR-ABB-11": "b0010007-0007-0007-0007-000000000007",
    "VAR-HIT-SJ200": "b0010008-0008-0008-0008-000000000008",
    "VAR-VEI-SI23": "b0020001-0001-0001-0001-000000000001",
    "VAR-GD-100PV": "b0020002-0002-0002-0002-000000000002",
    "MOT-PV": "b0020003-0003-0003-0003-000000000003",
    "POM-PV": "b0020004-0004-0004-0004-000000000004",
    "CAP-PV": "b0020005-0005-0005-0005-000000000005",
    "SEN-EAU": "b0020006-0006-0006-0006-000000000006",
}

ADMIN = "11111111-1111-1111-1111-111111111111"
RESP = "22222222-2222-2222-2222-222222222222"
TECH = "33333333-3333-3333-3333-333333333333"

# (code, description, equipment, zone, criticite, symptomes, cause, actions, analyse)
ENTRIES: list[tuple] = []

def add(code, desc, eq, zone, crit, symp, cause, actions, analyse=""):
    ENTRIES.append((code, desc, eq, zone, crit, symp, cause, actions, analyse or f"Diagnostic constructeur code {code}."))

# --- ABB ACS880 Spinning (26) ---
SPIN = "VAR-ACS-SPIN"
TRV = "VAR-ACS-TRV"
add("2310", "Surintensite sortie Overcurrent filature", SPIN, "Zone Filature", "HAUTE",
    "Code 2310 affiche sur HMI variateur filature, arret moteur",
    "Charge moteur excessive, rampe acceleration trop rapide, defaut cablage ou encodeur",
    "Verifier charge moteur; ajuster rampes grp 23/26/28; controler cable moteur et phasage; param groupe 99; cable encodeur",
    "Parametres grp 99 et rampes acc/dec filature ABB ACS880.")
add("2340", "Court-circuit Short circuit", SPIN, "Zone Filature", "CRITIQUE",
    "Defaut 2340, disjonction immediate variateur",
    "Court-circuit sortie variateur ou cablage moteur defectueux",
    "Controler cablage moteur; param 99.10; pas de condensateurs sur cable moteur; reboot carte commande 96.08",
    "Inspection isolement et continuite phases U/V/W.")
add("2330", "Defaut terre Earth leakage", SPIN, "Zone Filature", "HAUTE",
    "Code 2330, variateur en defaut terre",
    "Fuite isolement moteur ou cable vers terre",
    "Mesurer isolation moteur et cable; retirer condensateurs/absorbeurs; essai scalar control 99.04 si autorise",
    "Megohmetre moteur et cable avant remise sous tension.")
add("3381", "Phase moteur manquante Output phase loss", SPIN, "Zone Filature", "HAUTE",
    "Defaut 3381, une phase moteur absente",
    "Cable moteur mal connecte ou phase ouverte",
    "Connecter cable moteur 3 phases U/V/W",
    "Controle continuite et serrage bornes sortie variateur.")
add("4210", "Surchauffe IGBT", SPIN, "Zone Filature", "HAUTE",
    "Defaut 4210, temperature module puissance elevee",
    "Ventilation insuffisante, poussiere radiateur, surcharge",
    "Verifier ambiance, ventilation, radiateur; dimensionner puissance moteur vs variateur",
    "Nettoyage radiateur et verification ventilateurs ACS880.")
add("3210", "Surtension bus CC DC link overvoltage", SPIN, "Zone Filature", "HAUTE",
    "Defaut 3210 sur bus CC",
    "Energie recuperation excessive, alimentation instable",
    "Activer controle survoltage 30.30; verifier alimentation; chopper freinage; rampe deceleration",
    "Param 30.30 et dimensionnement freinage recuperatif.")
add("3220", "Sous-tension bus CC DC link undervoltage", SPIN, "Zone Filature", "MOYENNE",
    "Defaut 3220, tension bus insuffisante",
    "Alimentation reseau faible, fusible, phase manquante",
    "Verifier cablage, fusibles, redresseur; phase reseau manquante",
    "Mesure tensions d'alimentation R/S/T.")
add("3130", "Perte phase entree Input phase loss", SPIN, "Zone Filature", "HAUTE",
    "Defaut 3130 a la mise sous tension",
    "Phase reseau manquante ou desequilibre",
    "Controler fusibles, connexions, desequilibre reseau",
    "Verification triphase amont variateur.")
add("3385", "Echec autophasing", SPIN, "Zone Filature", "MOYENNE",
    "Defaut 3385 autophasing echoue",
    "Encodeur Z absent ou parametrage autophasing incorrect",
    "Changer mode autophasing 21.13; verifier impulsion Z encodeur; ID run moteur; param 99.03",
    "Module encodeur FEN-xx et impulsion index.")
add("5081", "Ventilateur auxiliaire HS Auxiliary fan broken", SPIN, "Zone Filature", "MOYENNE",
    "Alarme 5081 ventilateur auxiliaire",
    "Ventilateur auxiliaire bloque ou deconnecte",
    "Remplacer/reconnecter ventilateur auxiliaire; couvercle module en place",
    "Controle rotation ventilateur auxiliaire R3/R6.")
add("AFE1", "Arret urgence Off2 Emergency stop", SPIN, "Zone Filature", "CRITIQUE",
    "Defaut AFE1 arret urgence actif",
    "Circuit securite XSTO ou E-Stop active",
    "Reinitialiser arret urgence; verifier 21.05 et cablage securite",
    "Verification chaine securite STO avant Run.")
add("AFE2", "Arret urgence Off1/Off3", SPIN, "Zone Filature", "CRITIQUE",
    "Defaut AFE2 arret urgence",
    "Source stop securite active",
    "Verifier source stop et securite avant redemarrage",
    "Controle entrees securite variateur.")
add("A581", "Ventilateur principal bloque Fan filature", SPIN, "Zone Filature", "MOYENNE",
    "Alarme A581 ventilateur principal",
    "Ventilateur refroidissement bloque ou HS",
    "Verifier param 95.20; identifier ventilateur; remplacer si HS",
    "Param 95.20 et maintenance ventilateur principal.")
add("A780", "Moteur bloque Motor stall", SPIN, "Zone Filature", "HAUTE",
    "Alarme A780 moteur bloque ou surcharge",
    "Charge mecanique excessive ou moteur grippé",
    "Verifier charge et dimensionnement; param protection 31.24",
    "Couple charge filature vs courant nominal moteur.")
add("A798", "Perte communication encodeur Encoder comm loss", SPIN, "Zone Filature", "HAUTE",
    "Alarme A798 perte encodeur",
    "Module FEN deconnecte ou cable encodeur defectueux",
    "Verifier module FEN en slot; param 90.41/90.51; cable encodeur; fibre FEA-03",
    "Diagnostic chaine retour vitesse encodeur.")
add("A2B4", "Court-circuit warning Short circuit", SPIN, "Zone Filature", "MOYENNE",
    "Warning A2B4 court-circuit",
    "Anomalie isolement ou cablage moteur",
    "Verifier cablage; pas de condensateurs sur cable moteur",
    "Warning avant defaut bloquant 2340.")
add("A3A1", "Surtension bus CC warning DC link overvoltage", SPIN, "Zone Filature", "MOYENNE",
    "Warning A3A1 surtension bus",
    "Tension reseau ou bus elevee",
    "Verifier param 95.01 Supply voltage et tension reseau",
    "Surveillance tension bus avant defaut 3210.")
add("A497", "Temperature moteur 1 Motor temperature", SPIN, "Zone Filature", "MOYENNE",
    "Alarme A497 temperature moteur",
    "Surchauffe moteur ou capteur thermique",
    "Verifier refroidissement moteur, charge, cablage thermistance",
    "Capteur temperature moteur filature.")
add("A7A1", "Echec fermeture frein mecanique", SPIN, "Zone Filature", "MOYENNE",
    "Alarme A7A1 frein ne ferme pas",
    "Defaut cablage frein ou param groupe 44",
    "Verifier cablage frein; param groupe 44; signal acquittement",
    "Frein mecanique filature et groupe param 44.")
add("A7A2", "Echec ouverture frein mecanique traverse", TRV, "Zone Filature", "MOYENNE",
    "Alarme A7A2 frein traverse ne s'ouvre pas",
    "Incoherence signal ack frein",
    "Verifier groupe 44; coherence signal ack et etat frein",
    "Frein mecanique traverse.")
add("A5A0", "Safe Torque Off STO", SPIN, "Zone Filature", "CRITIQUE",
    "Alarme A5A0 STO actif",
    "Entree securite STO active",
    "Verifier circuit securite STO; param 31.22",
    "Entree XSTO variateur ACS880.")
add("AFEB", "Run enable manquant", SPIN, "Zone Filature", "MOYENNE",
    "Alarme AFEB Run enable absent",
    "Commande marche non autorisee par automate",
    "Verifier param 20.12 Run enable; cablage; mot commande fieldbus",
    "Interface automate/fieldbus filature.")
add("D200", "Somme sections pattern different de 100 pourcent", SPIN, "Zone Filature", "FAIBLE",
    "Defaut D200 pattern filature",
    "Sections Length grp 75-78 ne totalisent pas 100%",
    "Ajuster sections pattern Length pour total 100%",
    "Parametres pattern filature grp 75-78.")
add("D201", "Fin de bobine Doff end", SPIN, "Zone Filature", "FAIBLE",
    "Indication D201 fin doff",
    "Cycle doff termine selon param 81.06 ou 81.07",
    "Verifier param 81.06 temps ou 81.07 longueur doff; procedure operateur",
    "Cycle doff filature ACS880 +N5500.")
add("2310-TRV", "Surintensite traverse Overcurrent", TRV, "Zone Filature", "HAUTE",
    "Code 2310 sur variateur traverse",
    "Charge mecanique traverse excessive",
    "Verifier charge traverse; rampes 88.xx; cable moteur traverse",
    "Param rampes grp 88 traverse.")
add("A581-TRV", "Ventilateur traverse bloque", TRV, "Zone Filature", "MOYENNE",
    "Alarme A581 ventilateur traverse",
    "Ventilateur ou encrassement armoire",
    "Remplacer ventilateur; depoussiérer armoire traverse",
    "Refroidissement variateur traverse.")

# --- Hitachi SJ200 (24) ---
HIT = "VAR-HIT-SJ200"
add("E01", "Surintensite vitesse constante", HIT, "Zone Convoyage", "HAUTE",
    "Code E01 sur afficheur SJ200",
    "Court-circuit sortie, moteur grippé ou surcharge",
    "Verifier CC sortie et charge; cablage bi-tension; STOP/RESET puis corriger cause",
    "Journal defauts D081-D083 Hitachi.")
add("E02", "Surintensite deceleration", HIT, "Zone Convoyage", "HAUTE",
    "Code E02 en deceleration",
    "Recuperation energetique excessive",
    "Allonger deceleration; reduire energie recuperee",
    "Param temps deceleration SJ200.")
add("E03", "Surintensite acceleration", HIT, "Zone Convoyage", "HAUTE",
    "Code E03 en acceleration",
    "Rampe acceleration trop rapide ou surcharge",
    "Allonger acceleration; reduire charge; parametres moteur",
    "Param ACC Hitachi groupe A.")
add("E04", "Surintensite autres conditions", HIT, "Zone Convoyage", "HAUTE",
    "Code E04 surintensite",
    "Charge transitoire anormale",
    "Diagnostiquer charge; verifier cablage et terre",
    "Analyse charge convoyeur.")
add("E05", "Surcharge moteur", HIT, "Zone Convoyage", "MOYENNE",
    "Code E05 protection thermique moteur",
    "Surcharge moteur detectee electroniquement",
    "Reduire charge; ajuster protection thermique; courant nominal moteur",
    "Protection thermique electronique SJ200.")
add("E06", "Surcharge resistance freinage", HIT, "Zone Convoyage", "MOYENNE",
    "Code E06 freinage",
    "Resistance freinage depasse duty cycle",
    "Reduire frequence freinage; augmenter resistance ou duty cycle",
    "Dimensionnement resistance de freinage.")
add("E07", "Surtension bus CC", HIT, "Zone Convoyage", "HAUTE",
    "Code E07 surtension",
    "Energie recuperation sur bus CC",
    "Allonger DEC; installer/regler freinage",
    "Bus CC variateur Hitachi.")
add("E08", "Erreur EEPROM", HIT, "Zone Convoyage", "MOYENNE",
    "Code E08 memoire EEPROM",
    "Parasites ou temperature excessive memoire",
    "Verifier parametres; ne pas couper alim avec RS active; reset usine si persistant",
    "NOTA Hitachi: RS actif a coupure provoque E08.")
add("E09", "Sous-tension", HIT, "Zone Convoyage", "MOYENNE",
    "Code E09 sous-tension bus",
    "Chute tension alimentation",
    "Verifier alimentation entree et bus CC",
    "Mesure tension entree monophase/triphase.")
add("E11", "Erreur microprocesseur", HIT, "Zone Convoyage", "CRITIQUE",
    "Code E11/E22 microprocesseur",
    "Dysfonctionnement processeur interne",
    "Redemarrer; remplacement variateur ou SAV Hitachi si persistant",
    "Defaut interne variateur SJ200.")
add("E12", "Defaut exterieur EXT", HIT, "Zone Convoyage", "MOYENNE",
    "Code E12 entree EXT",
    "Signal present sur entree intelligente EXT",
    "Verifier entree EXT; retirer signal externe",
    "Configuration entrees intelligentes SJ200.")
add("E13", "USP demarrage intempestif", HIT, "Zone Convoyage", "MOYENNE",
    "Code E13 USP",
    "Run present a mise sous tension avec USP actif",
    "Effacer defaut; ne pas mettre Run a mise sous tension",
    "Protection USP Hitachi.")
add("E14", "Defaut de terre", HIT, "Zone Convoyage", "HAUTE",
    "Code E14 defaut terre",
    "Fuite terre sortie vers moteur",
    "Verifier isolement moteur/cable; corriger fuite phase-terre",
    "Test terre a mise sous tension SJ200.")
add("E15", "Surtension entree", HIT, "Zone Convoyage", "HAUTE",
    "Code E15 surtension entree",
    "Tension reseau excessive apres test 100s Stop",
    "Verifier tension reseau; attendre fin test",
    "Test surtension entree mode Stop.")
add("E21", "Disjonction thermique variateur", HIT, "Zone Convoyage", "HAUTE",
    "Code E21 thermique variateur",
    "Temperature interne excessive",
    "Nettoyer radiateur/ventilateur; temp ambiante -10 a 40C",
    "Maintenance ventilation SJ200.")
add("E23", "Erreur circuit logique", HIT, "Zone Convoyage", "CRITIQUE",
    "Code E23 circuit logique",
    "Erreur communication microprocesseur-CI logique",
    "Redemarrer; remplacer carte si persistant",
    "Defaut interne carte commande.")
add("E35", "Surchauffe moteur", HIT, "Zone Convoyage", "MOYENNE",
    "Code E35 surchauffe moteur",
    "Sonde thermique moteur entrees 6/L",
    "Verifier sonde [6]/[L]; reduire charge",
    "Entree sonde thermique moteur SJ200.")
add("E60", "Erreur communications", HIT, "Zone Convoyage", "MOYENNE",
    "Code E60 communications",
    "Watchdog reseau debordé",
    "Verifier RS485/Modbus; parametres communication",
    "Annexe B ModBus SJ200.")
add("E-SUBV", "Sous-tension coupure sortie", HIT, "Zone Convoyage", "MOYENNE",
    "Alarme sous-tension avec coupure sortie",
    "Tension entree faible, tentative redemarrage auto",
    "Stabiliser alimentation; variateur tente redemarrage auto",
    "Comportement basse tension SJ200.")
add("HIT-NODEM", "Moteur ne demarre pas", HIT, "Zone Convoyage", "MOYENNE",
    "Moteur ne demarre pas malgre Run",
    "F001 zero, RS/FRS actif, charge ou cablage FW/RV",
    "Verifier Run, F001>0, FW/RV, RS/FRS, charge",
    "Depannage ch 6-4 Hitachi SJ200.")
add("HIT-REV", "Sens rotation inverse", HIT, "Zone Convoyage", "FAIBLE",
    "Moteur tourne en sens inverse",
    "Phases U-V-W incorrectes ou F004",
    "Permuter U-V-W ou ajuster F004",
    "Param F004 sens rotation.")
add("HIT-SPD", "Vitesse n atteint pas consigne", HIT, "Zone Convoyage", "MOYENNE",
    "Frequence sortie inferieure a consigne",
    "Entree analogique, charge ou limite A004/A061",
    "Verifier entree analogique, charge, A004/A061",
    "Limites frequence SJ200 A004 A061.")
add("HIT-UNST", "Rotation instable", HIT, "Zone Convoyage", "MOYENNE",
    "Vitesse instable sous charge",
    "Fluctuation charge ou alimentation instable",
    "Reduire fluctuation charge; frequence de saut; stabiliser alimentation",
    "Param frequence de saut SJ200.")
add("HIT-PARAM", "Parametres non sauvegardes", HIT, "Zone Convoyage", "FAIBLE",
    "Parametre revient a ancienne valeur",
    "Store non appuye ou mode Run actif",
    "Appuyer Store; attendre >=6s avant remise sous tension",
    "Procedure sauvegarde parametres SJ200.")

# --- VEICHI SI23 (26) ---
VEI = "VAR-VEI-SI23"
add("E.LU2", "Sous-tension en marche", VEI, "Station PV", "MOYENNE",
    "Code E.LU2 sous-tension",
    "Tension entree faible ou contacteur DC ne ferme pas",
    "Verifier tension entree; contacteur DC principal",
    "Alimentation PV/triphase variateur VEICHI.")
add("E.oU1", "Surtension acceleration", VEI, "Station PV", "HAUTE",
    "Code E.oU1 surtension ACC",
    "Fluctuation tension alimentation",
    "Verifier reseau electrique / tension PV",
    "Stabilite tension entree SI23.")
add("E.oU2", "Surtension deceleration", VEI, "Station PV", "HAUTE",
    "Code E.oU2 surtension DEC",
    "DEC trop court ou charge lourde",
    "Prolonger DEC; reduire charge; freinage si besoin",
    "Param F00.15 deceleration.")
add("E.oU3", "Surtension vitesse constante", VEI, "Station PV", "HAUTE",
    "Code E.oU3 surtension constante",
    "Tension entree elevee ou force externe",
    "Ajuster tension normale; annuler force externe ou resistance freinage",
    "Tension nominale entree VEICHI.")
add("E.oU4", "Surtension a l arret", VEI, "Station PV", "MOYENNE",
    "Code E.oU4 surtension arret",
    "Fluctuation tension superieure limite",
    "Verifier tension entree",
    "Controle tension a l arret.")
add("E.oC1", "Surintensite acceleration", VEI, "Station PV", "HAUTE",
    "Code E.oC1 surintensite ACC",
    "ACC trop court, V/F incorrect, moteur bloque",
    "Prolonger ACC; regler V/F et couple; verifier cable et moteur bloque",
    "Param F00.14 ACC et courbe V/F.")
add("E.oC2", "Surintensite deceleration", VEI, "Station PV", "HAUTE",
    "Code E.oC2 surintensite DEC",
    "CC/terre sortie, auto-reglage moteur absent",
    "Eliminer CC/terre; auto-reglage moteur; augmenter ACC/DEC",
    "Auto-reglage moteur VEICHI.")
add("E.oC3", "Surintensite vitesse constante", VEI, "Station PV", "HAUTE",
    "Code E.oC3 surintensite constante",
    "Charge soudaine ou variateur sous-dimensionne",
    "Retirer charge soudaine; variateur plus puissant si besoin",
    "Dimensionnement 4 kW SI23-D5-004G.")
add("E.oL1", "Surcharge moteur", VEI, "Station PV", "MOYENNE",
    "Code E.oL1 surcharge moteur",
    "Couple eleve, ACC/DEC courts, param moteur incorrect",
    "Reduire couple; augmenter ACC/DEC; reset param moteur",
    "Parametres plaque moteur async.")
add("E.oL2", "Surcharge variateur", VEI, "Station PV", "HAUTE",
    "Code E.oL2 surcharge variateur",
    "Charge excessive ou variateur surcharge",
    "Reduire couple; verifier charge; remplacer par modele plus puissant",
    "Courant nominal variateur 4 kW.")
add("E.SC", "Systeme anormal", VEI, "Station PV", "CRITIQUE",
    "Code E.SC systeme anormal",
    "DEC trop court, CC sortie, carte commande",
    "Prolonger DEC; verifier CC sortie; CEM; support technique",
    "Defaut systeme interne VEICHI.")
add("E.oH1", "Surchauffe variateur", VEI, "Station PV", "HAUTE",
    "Code E.oH1 surchauffe",
    "Canal air bloque ou ventilateur HS",
    "Nettoyer canal air; ventilateur; reconnecter cables",
    "F04.28 commande ventilateur.")
add("E.oH2", "Surchauffe redresseur", VEI, "Station PV", "HAUTE",
    "Code E.oH2 surchauffe redresseur",
    "Temperature elevee canal air",
    "Nettoyer canal air; ventilateur; reconnecter cables",
    "Refroidissement redresseur SI23.")
add("E.EEP", "Defaut memoire EEPROM", VEI, "Station PV", "MOYENNE",
    "Code E.EEP defaut memoire",
    "Perturbation EM ou EEPROM endommagee",
    "Recharger/sauvegarder parametres; contact usine",
    "Sauvegarde parametres F00.19.")
add("E.ILF", "Perte phase entree", VEI, "Station PV", "HAUTE",
    "Code E.ILF perte phase entree",
    "Phase entree ouverte",
    "Verifier cablage triphase alimentation",
    "Entree R/S/T variateur.")
add("E.oLF", "Perte phase sortie", VEI, "Station PV", "HAUTE",
    "Code E.oLF perte phase sortie",
    "Phase sortie ouverte",
    "Verifier U/V/W sortie et cablage moteur",
    "Sortie moteur U/V/W.")
add("E.HAL", "Detection courant anormal", VEI, "Station PV", "HAUTE",
    "Code E.HAL courant anormal",
    "Desequilibre phase ou defaut circuit",
    "Verifier moteur et cablage; support usine",
    "Detection courant defaut VEICHI.")
add("E.PAn", "Defaut clavier", VEI, "Station PV", "FAIBLE",
    "Code E.PAn defaut clavier",
    "Cable clavier ou composants HS",
    "Verifier cable clavier RJ45; remplacer clavier",
    "Keypad externe RJ45 SI23.")
add("A.LPn", "Mode sommeil tension PV basse", VEI, "Station PV", "FAIBLE",
    "Alarme A.LPn mode sommeil",
    "Tension PV inferieure F14.11",
    "Attendre remontee > F14.12; ajuster F14.11/F14.13",
    "Param F14.11 Go to sleep et F14.12 Wake up.")
add("A.LFr", "Basse frequence", VEI, "Station PV", "MOYENNE",
    "Alarme A.LFr basse frequence",
    "Frequence sortie inferieure F14.14",
    "Frequence > F14.16 pour reprise auto",
    "Param F14.14 frequence minimale.")
add("A.LuT", "Marche a sec", VEI, "Station PV", "MOYENNE",
    "Alarme A.LuT marche a sec",
    "Courant sortie inferieur F14.17",
    "Verifier niveau eau; F14.17/F14.18; relais TA-TC manque d eau",
    "Protection marche a sec F14.17-F14.19.")
add("A.oLd", "Surintensite protection", VEI, "Station PV", "MOYENNE",
    "Alarme A.oLd surintensite",
    "Courant superieur F14.20",
    "Ajuster F14.20/F14.21; attendre F14.22 reprise",
    "Protection surintensite F14.20-F14.22.")
add("A.LPr", "Puissance minimale", VEI, "Station PV", "FAIBLE",
    "Alarme A.LPr puissance minimale",
    "Puissance inferieure F14.23",
    "Verifier ensoleillement; F14.23/F14.25",
    "Protection puissance min F14.23.")
add("VEI-DEBIT", "Debit eau faible bon ensoleillement", "POM-PV", "Station PV", "MOYENNE",
    "Debit eau tres faible malgre bon ensoleillement",
    "Sens rotation moteur inverse",
    "Verifier sens rotation moteur pompe",
    "FAQ pompe VEICHI SI23.")
add("VEI-VEILLE", "Variateur en veille 0 Hz", VEI, "Station PV", "MOYENNE",
    "Variateur reste en veille 0 Hz",
    "Commande X1 ou cablage commutateur",
    "Verifier commande X1; test a vide; reset F00.19",
    "Param F00.02 commande externe.")
add("VEI-DC", "Courant DC mal affiche", VEI, "Station PV", "FAIBLE",
    "Courant DC affichage incorrect",
    "Etalonnage F14.30/F14.31 incorrect",
    "Etalonner F14.30 et F14.31",
    "Etalonnage courant DC PV.")

# --- Goodrive 100-PV (39) ---
GD = "VAR-GD-100PV"
gd_entries = [
    ("OUt1", "Protection phase U variateur", "Acceleration trop rapide ou IGBT endommage", "Augmenter ACC; changer circuit puissance; verifier cables CEM"),
    ("OUt2", "Protection phase V variateur", "Phase V IGBT ou interference", "Idem OUt1; verifier cablage"),
    ("OUt3", "Protection phase W variateur", "Phase W IGBT ou charge anormale", "Idem OUt1; verifier charge"),
    ("OV1", "Surtension ACC", "Tension entree anormale", "Verifier puissance entree"),
    ("OV2", "Surtension DEC", "Retour energie ou DEC trop court", "Allonger DEC; freinage; reduire energie recuperee"),
    ("OV3", "Surtension vitesse constante", "Retour energie important", "Freinage; verifier charge"),
    ("OC1", "Surintensite ACC", "ACC rapide, tension faible, charge anormale", "Augmenter ACC; verifier entree; VF plus puissant"),
    ("OC2", "Surintensite DEC", "Charge transitoire ou CC", "Idem OC1; verifier CC/terre"),
    ("OC3", "Surintensite vitesse constante", "Surcharge ou interference", "Idem OC1; reduire interferences"),
    ("UV", "Sous-tension bus", "Alimentation ou PV faible", "Verifier ligne alimentation/PV"),
    ("OL1", "Surcharge moteur", "Courant nominal moteur incorrect", "Reset courant nominal; ajuster couple"),
    ("OL2", "Surcharge variateur", "ACC rapide ou charge lourde", "Augmenter ACC; VF plus puissant"),
    ("OL3", "Pre-alarme surcharge", "Seuil pre-alarme atteint", "Verifier charge et seuil pre-alarme"),
    ("SPI", "Perte phase entree", "Phase R/S/T manquante", "Verifier alimentation R-S-T"),
    ("SPO", "Perte phase sortie", "Phase U/V/W manquante", "Verifier moteur et cable"),
    ("OH1", "Surchauffe redresseur", "Canal air bloque", "Deboucher air; changer ventilateur"),
    ("OH2", "Surchauffe onduleur", "Temperature ambiante elevee", "Idem OH1; baisser temperature"),
    ("EF", "Defaut externe", "Entree externe active", "Verifier entree externe/relais"),
    ("CE", "Erreur communication", "Baud rate ou cablage RS485", "Regler baud; adresse; cablage"),
    ("ItE", "Defaut detection courant", "Panneau ou circuit HS", "Rebrancher panneau; remplacer carte"),
    ("tE", "Defaut autocalibrage", "Param moteur incorrect", "Param plaque moteur; vider charge; auto-reglage"),
    ("EEP", "Defaut EEPROM Goodrive", "EEPROM endommagee", "STOP/RST; remplacer carte controle"),
    ("PIDE", "Defaut retour PID", "Retour PID hors ligne", "Verifier signal et source PID"),
    ("END", "Temps usine depasse", "Compteur usine depasse", "Contacter fournisseur reset compteur"),
    ("ETH1", "Court-circuit terre 1", "Terre sortie court-circuitée", "Verifier cablage moteur; param moteur"),
    ("ETH2", "Court-circuit terre 2", "Detection terre defectueuse", "Remplacer carte si circuit HS"),
    ("dEu", "Deviation vitesse", "Charge bloquee", "Verifier charge; augmenter temps detection"),
    ("STo", "Defaut ajustement moteur sync", "Param moteur synchrone incorrect", "Verifier param sync et autosync"),
    ("LL", "Pre-alarme sous-charge", "Seuil sous-charge", "Verifier charge et seuil"),
    ("tSF", "Sonde hydraulique endommagee", "Sonde HS", "Remplacer sonde hydraulique", "SEN-EAU"),
    ("PINV", "Polarite PV inversee", "Cablag PV incorrect", "Inverser +/- PV", "CAP-PV"),
    ("PVOC", "Surintensite PV", "ACC/DCC rapide ou VF sous-dimensionne", "Augmenter ACC/DCC; VF plus puissant"),
    ("PVOV", "Surtension PV", "Trop panneaux en serie", "Reduire panneaux serie; verifier modele"),
    ("PVLv", "Sous-tension PV", "Puissance PV insuffisante", "Augmenter strings; tester plein soleil"),
    ("E-422", "Communication module boost 422", "Cables 422 incorrects", "Verifier 4 fils communication boost"),
    ("OV", "Surtension bus boost", "Changement brusque ensoleillement", "Ajuster P19.07 et P19.08"),
    ("A-LS", "Alerte faible lumiere", "Ensoleillement faible", "Attendre ensoleillement; config panneaux"),
    ("A-LL", "Alarme sous-charge reservoir vide", "Reservoir vide", "Verifier reservoir", "POM-PV"),
    ("A-tF", "Alarme plein eau", "Reservoir plein", "Verifier cablage alarme plein", "POM-PV"),
    ("A-tL", "Alerte manque d eau", "Reservoir vide", "Verifier reservoir et relais", "POM-PV"),
]
for code, desc, cause, actions, *rest in gd_entries:
    eq = rest[0] if rest else GD
    add(code, desc, eq, "Station PV", "MOYENNE" if code.startswith("A-") else "HAUTE",
        f"Code {code} affiche sur Goodrive 100-PV",
        cause, actions, f"Diagnostic ch 7 Goodrive 100-PV code {code}.")

# --- ACS880-11 maintenance (3) ---
ABB11 = "VAR-ABB-11"
add("MAINT-HEAT", "Surchauffe encrassement variateur", ABB11, "Zone Process", "MOYENNE",
    "Temperature elevee radiateur ACS880-11",
    "Radiateur ou ventilateurs encrasses",
    "Nettoyer radiateur et ventilateurs R3/R6/R8; remplacer ventilateur si HS",
    "Maintenance ch 11 ACS880-11 HW.")
add("MAINT-CAP", "Condensateurs vieillissants", ABB11, "Zone Process", "MOYENNE",
    "Inspection condensateurs requise",
    "Vieillissement condensateurs par temperature",
    "Inspection annuelle; reactiver condensateurs apres stockage",
    "Courbe duree de vie condensateurs ABB.")
add("MAINT-ISO", "Defaut isolement terre", ABB11, "Zone Process", "HAUTE",
    "Isolement moteur ou cable insuffisant",
    "Defaut isolement avant mise sous tension",
    "Mesurer isolement moteur/cable avant MES procedure ch 6",
    "Mesure isolement ACS880-11 installation manual.")


def sql_escape(s: str) -> str:
    return s.replace("'", "''")


def failure_uuid(n: int) -> str:
    return f"c{n:03d}0001-0001-0001-0001-{n:012d}"


def intervention_uuid(n: int) -> str:
    return f"d{n:03d}0001-0001-0001-0001-{n:012d}"


def generate_sql() -> str:
    lines = [
        "-- V23: Failures and validated interventions from manufacturer PDF manuals",
        "-- Generated by scripts/generate-pdf-seed.py — do not edit by hand",
        f"-- Total entries: {len(ENTRIES)}",
        "",
    ]

    failure_rows = []
    intervention_rows = []

    for i, (code, desc, eq_code, zone, crit, symp, cause, actions, analyse) in enumerate(ENTRIES, 1):
        eq_id = EQUIP[eq_code]
        fid = failure_uuid(i)
        iid = intervention_uuid(i)
        month = (i % 12) + 1
        day = (i % 28) + 1
        date = f"2025-{month:02d}-{day:02d} 08:{(i % 60):02d}:00"
        val_date = f"2025-{month:02d}-{min(day+1,28):02d} 10:00:00"
        duree = 30 + (i % 90)
        temps = 20 + (i % 60)

        failure_rows.append(
            f"    ('{fid}', '{eq_id}', '{date}', '{crit}', '{zone}', "
            f"'{RESP}', '{ADMIN}', 'RESOLUE', "
            f"'{sql_escape(desc)}', '{sql_escape(code)}')"
        )
        intervention_rows.append(
            f"    ('{iid}', '{fid}', '{TECH}', "
            f"'{sql_escape(f'Intervention {desc}')}', "
            f"'{sql_escape(symp)}', "
            f"'{sql_escape(cause)}', "
            f"'{sql_escape(analyse)}', "
            f"'{sql_escape(actions)}', "
            f"NULL, {duree}, {temps}, 'VALIDEE', '{RESP}', '{val_date}', "
            f"'Procedure constructeur PDF validee pour RAG')"
        )

    lines.append("INSERT INTO failures (id, equipment_id, date_heure, criticite, zone_service,")
    lines.append("                      responsable_id, declarant_id, statut, description_initiale, code_defaut)")
    lines.append("VALUES")
    lines.append(",\n".join(failure_rows))
    lines.append("ON CONFLICT (id) DO NOTHING;")
    lines.append("")
    lines.append("INSERT INTO interventions (id, failure_id, technicien_id, description, symptomes, cause_racine,")
    lines.append("                           analyse_technique, actions_correctives, pieces_remplacees,")
    lines.append("                           duree_arret_minutes, temps_intervention_minutes, statut_validation,")
    lines.append("                           validateur_id, date_validation, commentaire_validation)")
    lines.append("VALUES")
    lines.append(",\n".join(intervention_rows))
    lines.append("ON CONFLICT (id) DO NOTHING;")

    return "\n".join(lines) + "\n"


def main() -> None:
    root = Path(__file__).resolve().parents[1]
    out = root / "backend/src/main/resources/db/migration/V23__seed_pdf_failures_interventions.sql"
    content = generate_sql()
    out.write_text(content, encoding="utf-8")
    print(f"Wrote {len(ENTRIES)} entries to {out}")
    print(f"File size: {out.stat().st_size} bytes")


if __name__ == "__main__":
    main()
