package com.admin.equipment.repo;

import com.admin.equipment.model.AnomalyAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AnomalyAlertRepository extends JpaRepository<AnomalyAlert, Long> {

    List<AnomalyAlert> findByEquipmentIdOrderByDetectedAtDesc(Long equipmentId);

    List<AnomalyAlert> findByStatusOrderByDetectedAtDesc(String status);

    @Query("SELECT a FROM AnomalyAlert a WHERE a.detectedAt >= :from ORDER BY a.detectedAt DESC")
    List<AnomalyAlert> findRecentAlerts(@Param("from") LocalDateTime from);

    @Query("SELECT a FROM AnomalyAlert a WHERE a.equipmentId = :eqId AND a.metricName = :metric AND a.detectedAt >= :from")
    List<AnomalyAlert> findRecentMetricAlerts(@Param("eqId") Long equipmentId,
                                               @Param("metric") String metricName,
                                               @Param("from") LocalDateTime from);
}
