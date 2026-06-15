package com.admin.equipment.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "health_score_snapshots", indexes = {
    @Index(name = "idx_hss_equipment_time", columnList = "equipment_id, snapshot_time")
})
public class HealthScoreSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    @Column(name = "snapshot_time", nullable = false)
    private LocalDateTime snapshotTime;

    @Column(name = "total_score", nullable = false)
    private Double totalScore;

    @Column(name = "metric_score")
    private Double metricScore;

    @Column(name = "fault_score")
    private Double faultScore;

    @Column(name = "maintenance_score")
    private Double maintenanceScore;

    @Column(name = "inspection_score")
    private Double inspectionScore;

    @Column(name = "metric_weight")
    private Double metricWeight;

    @Column(name = "fault_weight")
    private Double faultWeight;

    @Column(name = "maintenance_weight")
    private Double maintenanceWeight;

    @Column(name = "inspection_weight")
    private Double inspectionWeight;

    @Column(columnDefinition = "TEXT")
    private String factors;

    @Column(name = "risk_level", length = 16)
    private String riskLevel;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
    public LocalDateTime getSnapshotTime() { return snapshotTime; }
    public void setSnapshotTime(LocalDateTime snapshotTime) { this.snapshotTime = snapshotTime; }
    public Double getTotalScore() { return totalScore; }
    public void setTotalScore(Double totalScore) { this.totalScore = totalScore; }
    public Double getMetricScore() { return metricScore; }
    public void setMetricScore(Double metricScore) { this.metricScore = metricScore; }
    public Double getFaultScore() { return faultScore; }
    public void setFaultScore(Double faultScore) { this.faultScore = faultScore; }
    public Double getMaintenanceScore() { return maintenanceScore; }
    public void setMaintenanceScore(Double maintenanceScore) { this.maintenanceScore = maintenanceScore; }
    public Double getInspectionScore() { return inspectionScore; }
    public void setInspectionScore(Double inspectionScore) { this.inspectionScore = inspectionScore; }
    public Double getMetricWeight() { return metricWeight; }
    public void setMetricWeight(Double metricWeight) { this.metricWeight = metricWeight; }
    public Double getFaultWeight() { return faultWeight; }
    public void setFaultWeight(Double faultWeight) { this.faultWeight = faultWeight; }
    public Double getMaintenanceWeight() { return maintenanceWeight; }
    public void setMaintenanceWeight(Double maintenanceWeight) { this.maintenanceWeight = maintenanceWeight; }
    public Double getInspectionWeight() { return inspectionWeight; }
    public void setInspectionWeight(Double inspectionWeight) { this.inspectionWeight = inspectionWeight; }
    public String getFactors() { return factors; }
    public void setFactors(String factors) { this.factors = factors; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
}
