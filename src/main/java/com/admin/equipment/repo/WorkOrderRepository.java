package com.admin.equipment.repo;

import com.admin.equipment.model.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    List<WorkOrder> findAllByOrderByIdDesc();
    List<WorkOrder> findByEquipmentIdOrderByIdDesc(Long equipmentId);
    List<WorkOrder> findByStatusOrderByIdDesc(String status);
    long countByStatus(String status);

    @Query("SELECT COUNT(w) FROM WorkOrder w WHERE w.equipmentId = :eqId AND w.type = 'repair' AND w.createdAt >= :from")
    long countRecentRepairs(@Param("eqId") Long equipmentId, @Param("from") LocalDateTime from);

    @Query("SELECT w FROM WorkOrder w WHERE w.equipmentId = :eqId AND w.type = 'maintenance' AND w.status = 'done' ORDER BY w.closedAt DESC")
    List<WorkOrder> findCompletedMaintenanceOrders(@Param("eqId") Long equipmentId);

    @Query("SELECT w FROM WorkOrder w WHERE w.equipmentId = :eqId AND w.type = 'repair' AND w.createdAt >= :from ORDER BY w.createdAt DESC")
    List<WorkOrder> findRecentRepairOrders(@Param("eqId") Long equipmentId, @Param("from") LocalDateTime from);
}
