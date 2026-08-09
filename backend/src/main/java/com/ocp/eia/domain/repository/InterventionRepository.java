package com.ocp.eia.domain.repository;

import com.ocp.eia.domain.model.Intervention;
import com.ocp.eia.domain.model.StatutValidation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
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

    @Query("""
            SELECT i.failure.id, COUNT(i)
            FROM Intervention i
            WHERE i.failure.id IN :ids
            GROUP BY i.failure.id
            """)
    List<Object[]> countByFailureIds(@Param("ids") Collection<UUID> ids);

    @Query("""
            SELECT i.failure.id, i.statutValidation
            FROM Intervention i
            WHERE i.failure.id IN :ids
            AND i.createdAt = (
                SELECT MAX(i2.createdAt) FROM Intervention i2 WHERE i2.failure.id = i.failure.id
            )
            """)
    List<Object[]> findLatestStatutByFailureIds(@Param("ids") Collection<UUID> ids);

    List<Intervention> findByFailureEquipmentIdOrderByCreatedAtDesc(UUID equipmentId);

    @Query("""
            SELECT DISTINCT i FROM Intervention i
            JOIN FETCH i.failure f
            JOIN FETCH f.equipment
            LEFT JOIN FETCH f.declarant
            LEFT JOIN FETCH f.responsable
            LEFT JOIN FETCH i.technicien
            LEFT JOIN FETCH i.validateur
            LEFT JOIN FETCH i.documents
            WHERE f.equipment.id = :equipmentId
            ORDER BY i.createdAt DESC
            """)
    List<Intervention> findByFailureEquipmentIdWithDetails(@Param("equipmentId") UUID equipmentId);

    @Query("""
            SELECT DISTINCT i FROM Intervention i
            JOIN FETCH i.failure f
            JOIN FETCH f.equipment
            LEFT JOIN FETCH f.declarant
            LEFT JOIN FETCH f.responsable
            LEFT JOIN FETCH i.technicien
            LEFT JOIN FETCH i.validateur
            LEFT JOIN FETCH i.documents
            WHERE i.id IN :ids
            """)
    List<Intervention> findAllByIdWithDetails(@Param("ids") List<UUID> ids);

    long countByStatutValidation(StatutValidation statutValidation);

    java.util.List<Intervention> findByStatutValidation(StatutValidation statutValidation);

    @Query("""
            SELECT i FROM Intervention i
            JOIN FETCH i.failure f
            JOIN FETCH f.equipment
            WHERE i.statutValidation = :statut
            """)
    List<Intervention> findByStatutValidationWithDetails(@Param("statut") StatutValidation statut);

    @Query("""
            SELECT i FROM Intervention i
            JOIN FETCH i.failure f
            JOIN FETCH f.equipment
            WHERE f.id = :failureId AND i.statutValidation = com.ocp.eia.domain.model.StatutValidation.VALIDEE
            """)
    List<Intervention> findValideeByFailureIdWithDetails(@Param("failureId") UUID failureId);

    @Query("""
            SELECT i FROM Intervention i
            JOIN FETCH i.failure f
            JOIN FETCH f.equipment e
            WHERE e.id = :equipmentId AND i.statutValidation = com.ocp.eia.domain.model.StatutValidation.VALIDEE
            """)
    List<Intervention> findValideeByEquipmentIdWithDetails(@Param("equipmentId") UUID equipmentId);

    @Query(value = """
            SELECT i.* FROM interventions i
            JOIN failures f ON f.id = i.failure_id
            JOIN equipment e ON e.id = f.equipment_id
            WHERE (:equipmentId IS NULL OR :equipmentId = '' OR e.id = CAST(:equipmentId AS uuid))
            AND (:statut IS NULL OR :statut = '' OR i.statut_validation = :statut)
            AND (:faultCode IS NULL OR :faultCode = '' OR f.code_defaut ILIKE CONCAT('%', :faultCode, '%'))
            AND (
                :textQuery IS NULL OR :textQuery = '' OR
                i.search_vector @@ plainto_tsquery('french', :textQuery)
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
                i.search_vector @@ plainto_tsquery('french', :textQuery)
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
