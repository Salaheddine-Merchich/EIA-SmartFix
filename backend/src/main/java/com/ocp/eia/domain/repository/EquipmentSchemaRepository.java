package com.ocp.eia.domain.repository;

import com.ocp.eia.domain.model.EquipmentSchema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EquipmentSchemaRepository extends JpaRepository<EquipmentSchema, UUID> {

    List<EquipmentSchema> findByEquipmentIdAndActiveTrueOrderByLabelAsc(UUID equipmentId);

    long countByActiveTrue();

    long countByEquipmentIdAndActiveTrue(UUID equipmentId);

    Optional<EquipmentSchema> findByIdAndEquipmentId(UUID id, UUID equipmentId);

    @Query("""
            SELECT es FROM EquipmentSchema es
            JOIN FETCH es.equipment e
            WHERE es.active = true
            ORDER BY e.code, es.label
            """)
    List<EquipmentSchema> findAllActiveWithEquipment();

    @Query("""
            SELECT es FROM EquipmentSchema es
            JOIN FETCH es.equipment e
            WHERE es.active = true
              AND (COALESCE(:zone, '') = '' OR LOWER(e.zone) LIKE LOWER(CONCAT('%', :zone, '%')))
              AND (COALESCE(:family, '') = '' OR LOWER(e.famille) LIKE LOWER(CONCAT('%', :family, '%')))
            ORDER BY e.code, es.label
            """)
    List<EquipmentSchema> findActiveByZoneAndFamily(
            @Param("zone") String zone,
            @Param("family") String family
    );
}
