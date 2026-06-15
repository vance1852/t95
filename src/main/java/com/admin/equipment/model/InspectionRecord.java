package com.admin.equipment.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inspection_records", indexes = {
    @Index(name = "idx_inspection_equipment", columnList = "equipment_id")
})
public class InspectionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    @Column(name = "work_order_id")
    private Long workOrderId;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(length = 16)
    private String result = "normal";

    @Column(name = "has_anomaly")
    private Boolean hasAnomaly = false;

    @Column(name = "anomaly_level", length = 16)
    private String anomalyLevel = "none";

    @Column(name = "anomaly_items", columnDefinition = "TEXT")
    private String anomalyItems;

    @Column(columnDefinition = "TEXT")
    private String description = "";

    @Column(name = "inspector", length = 64)
    private String inspector = "";

    @Column(name = "inspected_at")
    private LocalDateTime inspectedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
    public Long getWorkOrderId() { return workOrderId; }
    public void setWorkOrderId(Long workOrderId) { this.workOrderId = workOrderId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public Boolean getHasAnomaly() { return hasAnomaly; }
    public void setHasAnomaly(Boolean hasAnomaly) { this.hasAnomaly = hasAnomaly; }
    public String getAnomalyLevel() { return anomalyLevel; }
    public void setAnomalyLevel(String anomalyLevel) { this.anomalyLevel = anomalyLevel; }
    public String getAnomalyItems() { return anomalyItems; }
    public void setAnomalyItems(String anomalyItems) { this.anomalyItems = anomalyItems; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getInspector() { return inspector; }
    public void setInspector(String inspector) { this.inspector = inspector; }
    public LocalDateTime getInspectedAt() { return inspectedAt; }
    public void setInspectedAt(LocalDateTime inspectedAt) { this.inspectedAt = inspectedAt; }
}
