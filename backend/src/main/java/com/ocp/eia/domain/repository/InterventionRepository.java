package com.ocp.eia.domain.repository;

import com.ocp.eia.domain.model.Intervention;
import com.ocp.eia.domain.model.StatutValidation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterventionRepository extends JpaRepository<Intervention, UUID> {

    @Query("""
            SELECT DISTINCT i FROM Intervention i
            JOIN FETCH i.failure f
            JOIN FETCH f.equipment
            LEFT JOIN FETCH f.declarant
            LEFT JOIN FETCH f.responsable
            LEFT JOIN FETCH i.technicien
            LEFT JOIN FETCH i.validateur
            LEFT JOIN FETCH i.documents
            WHERE i.id = :id
            """)
    Optional<Intervention> findByIdWithDetails(@Param("id") UUID id);

    Page<Intervention> findByFailureId(UUID failureId, Pageable pageable);

    @EntityGraph(attributePaths = {
            "documents",
            "failure",
            "failure.equipment",
            "technicien",
            "validateur"
    })
    @Query("SELECT i FROM Intervention i WHERE i.failure.id = :failureId")
    Page<Intervention> findByFailureIdWithDocuments(@Param("failureId") UUID failureId, Pageable pageable);

    List<Intervention> findByFailureEquipmentIdOrderByCreatedAtDesc(UUID equipmentId);

    long countByStatutValidation(StatutValidation statutValidation);

    java.util.List<Intervention> findByStatutValidation(StatutValidation statutValidation);

    @Query("""
            SELECT i FROM Intervention i
            JOIN FETCH i.failure f
            JOIN FETCH f.equipment
            WHERE i.statutValidation = :statut
            """)
    List<Intervention> findByStatutValidationWithDetails(@Param("statut") StatutValidation statut);

    @Query(value = """
            SELECT i.* FROM interventions i
            JOIN failures f ON f.id = i.failure_id
            JOIN equipment e ON e.id = f.equipment_id
            WHERE (:equipmentId IS NULL OR :equipmentId = '' OR e.id = CAST(:equipmentId AS uuid))
            AND (:statut IS NULL OR :statut = '' OR i.statut_validation = :statut)
            AND (:faultCode IS NULL OR :faultCode = '' OR f.code_defaut ILIKE CONCAT('%', :faultCode, '%'))
            AND (
                :textQuery IS NULL OR :textQuery = '' OR
                to_tsvector('french',
                    coalesce(i.symptomes, '') || ' ' ||
                    coalesce(i.cause_racine, '') || ' ' ||
                    coalesce(i.actions_correctives, '') || ' ' ||
                    coalesce(i.analyse_technique, '') || ' ' ||
                    coalesce(i.description, '') || ' ' ||
                    coalesce(f.description_initiale, '') || ' ' ||
                    coalesce(f.code_defaut, '') || ' ' ||
                    coalesce(e.code, '') || ' ' ||
                    coalesce(e.designation, '')
                ) @@ plainto_tsquery('french', :textQuery)
            )
            ORDER BY i.created_at DESC
            """, nativeQuery = true, countQuery = """
            SELECT count(*) FROM interventions i
            JOIN failures f ON f.id = i.failure_id
            JOIN equipment e ON e.id = f.equipment_id
            WHERE (:equipmentId IS NULL OR :equipmentId = '' OR e.id = CAST(:equipmentId AS uuid))
            AND (:statut IS NULL OR :statut = '' OR i.statut_validation = :statut)
            AND (:faultCode IS NULL OR :faultCode = '' OR f.code_defaut ILIKE CONCAT('%', :faultCode, '%'))
            AND (
                :textQuery IS NULL OR :textQuery = '' OR
                to_tsvector('french',
                    coalesce(i.symptomes, '') || ' ' ||
                    coalesce(i.cause_racine, '') || ' ' ||
                    coalesce(i.actions_correctives, '') || ' ' ||
                    coalesce(i.analyse_technique, '') || ' ' ||
                    coalesce(i.description, '') || ' ' ||
                    coalesce(f.description_initiale, '') || ' ' ||
                    coalesce(f.code_defaut, '') || ' ' ||
                    coalesce(e.code, '') || ' ' ||
                    coalesce(e.designation, '')
                ) @@ plainto_tsquery('french', :textQuery)
            )
            """)
    Page<Intervention> fullTextSearch(@Param("textQuery") String textQuery,
                                      @Param("equipmentId") String equipmentId,
                                      @Param("faultCode") String faultCode,
                                      @Param("statut") String statut,
                                      Pageable pageable);

    @Query(value = """
            SELECT i.cause_racine AS cause, COUNT(*) AS cnt
            FROM interventions i
            WHERE i.statut_validation = 'VALIDEE'
            AND i.cause_racine IS NOT NULL AND i.cause_racine <> ''
            GROUP BY i.cause_racine
            ORDER BY cnt DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopCauses(@Param("limit") int limit);

    @Query(value = """
            SELECT AVG(i.temps_intervention_minutes)
            FROM interventions i
            WHERE i.statut_validation = 'VALIDEE'
            AND i.temps_intervention_minutes IS NOT NULL
            """, nativeQuery = true)
    Double calculateMttr();

    @Query(value = """
            SELECT e.id, e.code, e.designation, COUNT(f.id) AS failure_count
            FROM equipment e
            JOIN failures f ON f.equipment_id = e.id
            GROUP BY e.id, e.code, e.designation
            ORDER BY failure_count DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopFailingEquipment(@Param("limit") int limit);

    @Query(value = """
            SELECT e.famille, COUNT(f.id) AS cnt
            FROM equipment e
            JOIN failures f ON f.equipment_id = e.id
            WHERE e.famille IS NOT NULL
            GROUP BY e.famille
            ORDER BY cnt DESC
            """, nativeQuery = true)
    List<Object[]> countFailuresByFamille();
}
