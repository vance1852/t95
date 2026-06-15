package com.admin.equipment.repo;

import com.admin.equipment.model.InspectionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface InspectionRecordRepository extends JpaRepository<InspectionRecord, Long> {

    List<InspectionRecord> findByEquipmentIdOrderByInspectedAtDesc(Long equipmentId);

    @Query("SELECT i FROM InspectionRecord i WHERE i.equipmentId = :eqId AND i.hasAnomaly = true AND i.inspectedAt >= :from ORDER BY i.inspectedAt DESC")
    List<InspectionRecord> findRecentAnomalies(@Param("eqId") Long equipmentId, @Param("from") LocalDateTime from);

    @Query("SELECT COUNT(i) FROM InspectionRecord i WHERE i.equipmentId = :eqId AND i.hasAnomaly = true AND i.inspectedAt >= :from")
    long countRecentAnomalies(@Param("eqId") Long equipmentId, @Param("from") LocalDateTime from);
}
