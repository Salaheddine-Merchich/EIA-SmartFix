package com.ocp.eia.modules.knowledge.evaluation;

import java.util.List;
import java.util.UUID;

/**
 * Jeu de scénarios métier représentatifs pour l'évaluation du RAG.
 * UUIDs alignés sur le seed PDF V23 (interventions validées).
 */
public final class RagEvaluationDataset {

    public static final UUID E21_HITACHI = UUID.fromString("d0410001-0001-0001-0001-000000000041");
    public static final UUID OUT1_GOODRIVE = UUID.fromString("d0770001-0001-0001-0001-000000000077");
    public static final UUID ABB_2310_FILATURE = UUID.fromString("d0010001-0001-0001-0001-000000000001");
    public static final UUID SURCHAUFFE_VARIATEUR = UUID.fromString("11111111-1111-1111-1111-111111111002");
    public static final UUID VIBRATION_ROULEMENT = UUID.fromString("11111111-1111-1111-1111-111111111003");
    public static final UUID ALARME_CAPTEUR = UUID.fromString("11111111-1111-1111-1111-111111111004");
    public static final UUID FUITE_HYDRAULIQUE = UUID.fromString("11111111-1111-1111-1111-111111111005");
    public static final UUID CODE_EQUIPEMENT = UUID.fromString("11111111-1111-1111-1111-111111111006");
    public static final UUID CONSTRUCTEUR_ABB = UUID.fromString("11111111-1111-1111-1111-111111111007");
    public static final UUID COURROIE_USURE = UUID.fromString("11111111-1111-1111-1111-111111111008");
    public static final UUID POMPE_PV_VEILLE = UUID.fromString("d0750001-0001-0001-0001-000000000075");
    public static final UUID POMPE_PV_SOMMEIL = UUID.fromString("d0690001-0001-0001-0001-000000000069");
    public static final UUID CONVOYEUR_ROTATION = UUID.fromString("d0470001-0001-0001-0001-000000000047");

    private RagEvaluationDataset() {
    }

    public static List<RagEvaluationCase> standardCases() {
        return List.of(
                new RagEvaluationCase(
                        "E21-hitachi-surchauffe",
                        "E21 surchauffe variateur Hitachi SJ200",
                        E21_HITACHI,
                        "Recherche exacte code E21 dans phrase longue"
                ),
                new RagEvaluationCase(
                        "OUt1-goodrive-phase-u",
                        "OUt1 protection phase U Goodrive",
                        OUT1_GOODRIVE,
                        "Recherche exacte code OUt1 Goodrive"
                ),
                new RagEvaluationCase(
                        "2310-abb-filature",
                        "2310 surintensité ABB filature",
                        ABB_2310_FILATURE,
                        "Recherche exacte code numérique ABB"
                ),
                new RagEvaluationCase(
                        "surchauffe-variateur",
                        "Le variateur chauffe anormalement et sent le brûlé",
                        SURCHAUFFE_VARIATEUR,
                        "Similarité sémantique sur symptôme variateur"
                ),
                new RagEvaluationCase(
                        "vibration-roulement",
                        "Vibration importante au niveau du moteur, bruit de roulement",
                        VIBRATION_ROULEMENT,
                        "Diagnostic mécanique roulement"
                ),
                new RagEvaluationCase(
                        "alarme-capteur",
                        "Alarme capteur de proximité défectueux sur ligne 3",
                        ALARME_CAPTEUR,
                        "Panne capteur / alarme"
                ),
                new RagEvaluationCase(
                        "fuite-hydraulique",
                        "Fuite d'huile au niveau du vérin hydraulique, joint usé",
                        FUITE_HYDRAULIQUE,
                        "Panne hydraulique"
                ),
                new RagEvaluationCase(
                        "code-equipement",
                        "Panne sur l'équipement EQ-CONV-042",
                        CODE_EQUIPEMENT,
                        "Recherche par code équipement ILIKE"
                ),
                new RagEvaluationCase(
                        "constructeur-abb",
                        "Variateur ABB en défaut de communication",
                        CONSTRUCTEUR_ABB,
                        "Recherche par constructeur"
                ),
                new RagEvaluationCase(
                        "courroie-usure",
                        "Courroie de convoyeur usée provoquant un glissement",
                        COURROIE_USURE,
                        "Usure courroie convoyage"
                ),
                new RagEvaluationCase(
                        "pompe-pv-no-start",
                        "Pompe PV ne démarre plus station solaire",
                        POMPE_PV_VEILLE,
                        "Requête sémantique pompe PV sans code défaut"
                ),
                new RagEvaluationCase(
                        "requete-hors-corpus",
                        "Panne sur compresseur frigorifique réversible modèle XYZ-9999",
                        null,
                        "Cas négatif — aucune intervention attendue"
                ),
                new RagEvaluationCase(
                        "F001-inconnu",
                        "F001 surchauffe convoyeur",
                        null,
                        "Code extrait absent de la base — aucune intervention attendue"
                )
        );
    }
}
