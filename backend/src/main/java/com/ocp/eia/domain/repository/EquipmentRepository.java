package com.ocp.eia.domain.repository;

import com.ocp.eia.domain.model.Equipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface EquipmentRepository extends JpaRepository<Equipment, UUID> {
    Optional<Equipment> findByCode(String code);
    boolean existsByCode(String code);

    @Query("""
            SELECT e FROM Equipment e
            WHERE (:search IS NULL OR :search = '' OR
                   e.code ILIKE CONCAT('%', :search, '%') OR
                   e.designation ILIKE CONCAT('%', :search, '%') OR
                   e.famille ILIKE CONCAT('%', :search, '%'))
            AND (:famille IS NULL OR :famille = '' OR e.famille = :famille)
            AND (:zone IS NULL OR :zone = '' OR e.zone = :zone)
            """)
    Page<Equipment> search(@Param("search") String search,
                           @Param("famille") String famille,
                           @Param("zone") String zone,
                           Pageable pageable);
}
