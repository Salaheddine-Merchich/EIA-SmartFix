package com.ocp.eia.modules.knowledge.evaluation;

import java.util.List;
import java.util.UUID;

/**
 * Jeu de scénarios métier représentatifs pour l'évaluation du RAG.
 * Les UUID attendus sont des placeholders : en intégration, remplacer par les IDs réels indexés.
 */
public final class RagEvaluationDataset {

    public static final UUID E001_CONVYEUR = UUID.fromString("11111111-1111-1111-1111-111111111001");
    public static final UUID SURCHAUFFE_VARIATEUR = UUID.fromString("11111111-1111-1111-1111-111111111002");
    public static final UUID VIBRATION_ROULEMENT = UUID.fromString("11111111-1111-1111-1111-111111111003");
    public static final UUID ALARME_CAPTEUR = UUID.fromString("11111111-1111-1111-1111-111111111004");
    public static final UUID FUITE_HYDRAULIQUE = UUID.fromString("11111111-1111-1111-1111-111111111005");
    public static final UUID CODE_EQUIPEMENT = UUID.fromString("11111111-1111-1111-1111-111111111006");
    public static final UUID CONSTRUCTEUR_ABB = UUID.fromString("11111111-1111-1111-1111-111111111007");
    public static final UUID COURROIE_USURE = UUID.fromString("11111111-1111-1111-1111-111111111008");

    private RagEvaluationDataset() {
    }

    public static List<RagEvaluationCase> standardCases() {
        return List.of(
                new RagEvaluationCase(
                        "E001-convoyeur-siemens",
                        "Le convoyeur Siemens affiche le code E001",
                        E001_CONVYEUR,
                        "Recherche par code défaut + constructeur"
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
                        "requete-hors-corpus",
                        "Panne sur compresseur frigorifique réversible modèle XYZ-9999",
                        null,
                        "Cas négatif — aucune intervention attendue"
                )
        );
    }
}
