package com.admin.equipment.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "predictive_maintenance_orders", indexes = {
    @Index(name = "idx_pmo_equipment", columnList = "equipment_id")
})
public class PredictiveMaintenanceOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    @Column(name = "work_order_id")
    private Long workOrderId;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "predicted_fault_date")
    private LocalDateTime predictedFaultDate;

    @Column(name = "remaining_health_days")
    private Integer remainingHealthDays;

    @Column(name = "current_health_score")
    private Double currentHealthScore;

    @Column(name = "health_score_trend", columnDefinition = "TEXT")
    private String healthScoreTrend;

    @Column(name = "risk_level", length = 16)
    private String riskLevel;

    @Column(length = 16)
    private String priority = "medium";

    @Column(length = 16)
    private String status = "pending";

    @Column(name = "suggested_actions", columnDefinition = "TEXT")
    private String suggestedActions;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "actual_fault_occurred")
    private Boolean actualFaultOccurred;

    @Column(name = "prediction_accuracy")
    private Double predictionAccuracy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
    public Long getWorkOrderId() { return workOrderId; }
    public void setWorkOrderId(Long workOrderId) { this.workOrderId = workOrderId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getPredictedFaultDate() { return predictedFaultDate; }
    public void setPredictedFaultDate(LocalDateTime predictedFaultDate) { this.predictedFaultDate = predictedFaultDate; }
    public Integer getRemainingHealthDays() { return remainingHealthDays; }
    public void setRemainingHealthDays(Integer remainingHealthDays) { this.remainingHealthDays = remainingHealthDays; }
    public Double getCurrentHealthScore() { return currentHealthScore; }
    public void setCurrentHealthScore(Double currentHealthScore) { this.currentHealthScore = currentHealthScore; }
    public String getHealthScoreTrend() { return healthScoreTrend; }
    public void setHealthScoreTrend(String healthScoreTrend) { this.healthScoreTrend = healthScoreTrend; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSuggestedActions() { return suggestedActions; }
    public void setSuggestedActions(String suggestedActions) { this.suggestedActions = suggestedActions; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public Boolean getActualFaultOccurred() { return actualFaultOccurred; }
    public void setActualFaultOccurred(Boolean actualFaultOccurred) { this.actualFaultOccurred = actualFaultOccurred; }
    public Double getPredictionAccuracy() { return predictionAccuracy; }
    public void setPredictionAccuracy(Double predictionAccuracy) { this.predictionAccuracy = predictionAccuracy; }
}
