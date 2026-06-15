package com.admin.equipment.repo;

import com.admin.equipment.model.EquipmentRuntimeData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EquipmentRuntimeDataRepository extends JpaRepository<EquipmentRuntimeData, Long> {

    List<EquipmentRuntimeData> findByEquipmentIdOrderByRecordedAtDesc(Long equipmentId);

    @Query("SELECT r FROM EquipmentRuntimeData r WHERE r.equipmentId = :eqId AND r.recordedAt >= :from ORDER BY r.recordedAt DESC")
    List<EquipmentRuntimeData> findByEquipmentIdAndRecordedAtAfter(@Param("eqId") Long equipmentId, @Param("from") LocalDateTime from);

    @Query("SELECT r FROM EquipmentRuntimeData r WHERE r.equipmentId = :eqId AND r.recordedAt BETWEEN :from AND :to ORDER BY r.recordedAt ASC")
    List<EquipmentRuntimeData> findByEquipmentIdAndRecordedAtBetween(@Param("eqId") Long equipmentId,
                                                                     @Param("from") LocalDateTime from,
                                                                     @Param("to") LocalDateTime to);

    @Query("SELECT r FROM EquipmentRuntimeData r WHERE r.equipmentId = :eqId ORDER BY r.recordedAt DESC LIMIT :limit")
    List<EquipmentRuntimeData> findLatestByEquipmentId(@Param("eqId") Long equipmentId, @Param("limit") int limit);
}
