package com.admin.equipment.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "prediction_feedbacks", indexes = {
    @Index(name = "idx_feedback_equipment", columnList = "equipment_id")
})
public class PredictionFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    @Column(name = "prediction_id")
    private Long predictionId;

    @Column(name = "predicted_date")
    private LocalDateTime predictedDate;

    @Column(name = "actual_date")
    private LocalDateTime actualDate;

    @Column(name = "days_deviation")
    private Integer daysDeviation;

    @Column(name = "fault_occurred")
    private Boolean faultOccurred;

    @Column(name = "accuracy_score")
    private Double accuracyScore;

    @Column(name = "feedback_type", length = 16)
    private String feedbackType = "fault";

    @Column(columnDefinition = "TEXT")
    private String remark;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
    public Long getPredictionId() { return predictionId; }
    public void setPredictionId(Long predictionId) { this.predictionId = predictionId; }
    public LocalDateTime getPredictedDate() { return predictedDate; }
    public void setPredictedDate(LocalDateTime predictedDate) { this.predictedDate = predictedDate; }
    public LocalDateTime getActualDate() { return actualDate; }
    public void setActualDate(LocalDateTime actualDate) { this.actualDate = actualDate; }
    public Integer getDaysDeviation() { return daysDeviation; }
    public void setDaysDeviation(Integer daysDeviation) { this.daysDeviation = daysDeviation; }
    public Boolean getFaultOccurred() { return faultOccurred; }
    public void setFaultOccurred(Boolean faultOccurred) { this.faultOccurred = faultOccurred; }
    public Double getAccuracyScore() { return accuracyScore; }
    public void setAccuracyScore(Double accuracyScore) { this.accuracyScore = accuracyScore; }
    public String getFeedbackType() { return feedbackType; }
    public void setFeedbackType(String feedbackType) { this.feedbackType = feedbackType; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
