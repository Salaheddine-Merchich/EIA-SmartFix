package com.ocp.eia.domain.repository;

import com.ocp.eia.domain.model.Criticite;
import com.ocp.eia.domain.model.Failure;
import com.ocp.eia.domain.model.StatutPanne;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FailureRepository extends JpaRepository<Failure, UUID> {

    @Query(value = """
            SELECT f FROM Failure f
            JOIN FETCH f.equipment e
            LEFT JOIN FETCH f.declarant d
            LEFT JOIN FETCH f.responsable r
            WHERE (:equipmentId IS NULL OR e.id = :equipmentId)
            AND (:statut IS NULL OR f.statut = :statut)
            AND (:criticite IS NULL OR f.criticite = :criticite)
            AND (:codeDefaut IS NULL OR :codeDefaut = '' OR LOWER(f.codeDefaut) LIKE LOWER(CONCAT('%', :codeDefaut, '%')))
            AND (:search IS NULL OR :search = '' OR
                 LOWER(f.descriptionInitiale) LIKE LOWER(CONCAT('%', :search, '%')) OR
                 LOWER(f.codeDefaut) LIKE LOWER(CONCAT('%', :search, '%')))
            """,
            countQuery = """
            SELECT COUNT(f) FROM Failure f
            JOIN f.equipment e
            WHERE (:equipmentId IS NULL OR e.id = :equipmentId)
            AND (:statut IS NULL OR f.statut = :statut)
            AND (:criticite IS NULL OR f.criticite = :criticite)
            AND (:codeDefaut IS NULL OR :codeDefaut = '' OR LOWER(f.codeDefaut) LIKE LOWER(CONCAT('%', :codeDefaut, '%')))
            AND (:search IS NULL OR :search = '' OR
                 LOWER(f.descriptionInitiale) LIKE LOWER(CONCAT('%', :search, '%')) OR
                 LOWER(f.codeDefaut) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Failure> search(@Param("equipmentId") UUID equipmentId,
                         @Param("statut") StatutPanne statut,
                         @Param("criticite") Criticite criticite,
                         @Param("codeDefaut") String codeDefaut,
                         @Param("search") String search,
                         Pageable pageable);

    @Query(value = """
            SELECT f FROM Failure f
            JOIN FETCH f.equipment e
            LEFT JOIN FETCH f.declarant d
            LEFT JOIN FETCH f.responsable r
            WHERE f.id = :id
            """)
    Optional<Failure> findByIdWithDetails(@Param("id") UUID id);

    List<Failure> findByEquipmentIdOrderByDateHeureDesc(UUID equipmentId);

    @Query("""
            SELECT DISTINCT f FROM Failure f
            JOIN FETCH f.equipment e
            LEFT JOIN FETCH f.declarant
            LEFT JOIN FETCH f.responsable
            LEFT JOIN FETCH f.interventions
            WHERE e.id = :equipmentId
            ORDER BY f.dateHeure DESC
            """)
    List<Failure> findByEquipmentIdWithDetails(@Param("equipmentId") UUID equipmentId);

    long countByEquipmentId(UUID equipmentId);

    long countByStatut(StatutPanne statut);
}
