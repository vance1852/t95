package com.admin.equipment.repo;

import com.admin.equipment.model.HealthScoreSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HealthScoreSnapshotRepository extends JpaRepository<HealthScoreSnapshot, Long> {

    List<HealthScoreSnapshot> findByEquipmentIdOrderBySnapshotTimeDesc(Long equipmentId);

    @Query("SELECT h FROM HealthScoreSnapshot h WHERE h.equipmentId = :eqId AND h.snapshotTime >= :from ORDER BY h.snapshotTime ASC")
    List<HealthScoreSnapshot> findByEquipmentIdAndSnapshotTimeAfter(@Param("eqId") Long equipmentId,
                                                                    @Param("from") LocalDateTime from);

    @Query("SELECT h FROM HealthScoreSnapshot h WHERE h.equipmentId = :eqId AND h.snapshotTime BETWEEN :from AND :to ORDER BY h.snapshotTime ASC")
    List<HealthScoreSnapshot> findByEquipmentIdAndSnapshotTimeBetween(@Param("eqId") Long equipmentId,
                                                                      @Param("from") LocalDateTime from,
                                                                      @Param("to") LocalDateTime to);

    @Query("SELECT h FROM HealthScoreSnapshot h WHERE h.equipmentId = :eqId ORDER BY h.snapshotTime DESC LIMIT 1")
    Optional<HealthScoreSnapshot> findLatestByEquipmentId(@Param("eqId") Long equipmentId);

    @Query("SELECT h FROM HealthScoreSnapshot h WHERE h.id IN (SELECT MAX(h2.id) FROM HealthScoreSnapshot h2 GROUP BY h2.equipmentId)")
    List<HealthScoreSnapshot> findLatestForAllEquipments();
}
