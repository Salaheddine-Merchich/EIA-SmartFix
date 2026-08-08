package com.ocp.eia.modules.knowledge.domain.model;

import java.util.UUID;

/**
 * Contexte pour prioriser la recherche RAG basée sur l'équipement et la panne.
 * Permet de filtrer et pondérer les résultats selon la proximité contextuelle.
 */
public record SearchContext(
        UUID equipmentId,
        UUID failureId,
        String equipmentFamily,
        String equipmentZone,
        Double equipmentBoost,
        Double familyBoost,
        Double zoneBoost
) {
    
    /**
     * Crée un contexte par défaut sans filtrage
     */
    public static SearchContext none() {
        return new SearchContext(null, null, null, null, 1.0, 1.0, 1.0);
    }
    
    /**
     * Crée un contexte avec les informations d'équipement et les boosts configurés
     */
    public static SearchContext of(UUID equipmentId, UUID failureId, String equipmentFamily, String equipmentZone) {
        return new SearchContext(equipmentId, failureId, equipmentFamily, equipmentZone, 2.0, 1.5, 1.2);
    }
    
    /**
     * Crée un contexte avec des boosts personnalisés
     */
    public static SearchContext withBoosts(UUID equipmentId, UUID failureId, String equipmentFamily, String equipmentZone,
                                         double equipmentBoost, double familyBoost, double zoneBoost) {
        return new SearchContext(equipmentId, failureId, equipmentFamily, equipmentZone, 
                                equipmentBoost, familyBoost, zoneBoost);
    }
    
    /**
     * Vérifie si ce contexte a des critères de filtrage
     */
    public boolean hasFilters() {
        return equipmentId != null || failureId != null || 
               (equipmentFamily != null && !equipmentFamily.isBlank()) ||
               (equipmentZone != null && !equipmentZone.isBlank());
    }
    
    /**
     * Calcule le boost à appliquer en fonction de la correspondance contextuelle
     */
    public double calculateBoost(UUID resultEquipmentId, String resultFamily, String resultZone) {
        if (equipmentId != null && equipmentId.equals(resultEquipmentId)) {
            return equipmentBoost;
        }
        
        if (equipmentFamily != null && equipmentFamily.equalsIgnoreCase(resultFamily)) {
            return familyBoost;
        }
        
        if (equipmentZone != null && equipmentZone.equalsIgnoreCase(resultZone)) {
            return zoneBoost;
        }
        
        return 1.0; // Pas de boost
    }
}