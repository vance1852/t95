package com.admin.equipment.repo;

import com.admin.equipment.model.PredictiveMaintenanceOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PredictiveMaintenanceOrderRepository extends JpaRepository<PredictiveMaintenanceOrder, Long> {

    List<PredictiveMaintenanceOrder> findByEquipmentIdOrderByGeneratedAtDesc(Long equipmentId);

    List<PredictiveMaintenanceOrder> findByStatusOrderByGeneratedAtDesc(String status);

    @Query("SELECT p FROM PredictiveMaintenanceOrder p WHERE p.status = 'pending' AND p.remainingHealthDays <= :days ORDER BY p.remainingHealthDays ASC")
    List<PredictiveMaintenanceOrder> findUrgentPredictions(@Param("days") int days);

    @Query("SELECT p FROM PredictiveMaintenanceOrder p WHERE p.generatedAt >= :from ORDER BY p.generatedAt DESC")
    List<PredictiveMaintenanceOrder> findRecentPredictions(@Param("from") LocalDateTime from);
}
