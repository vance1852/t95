package com.admin.equipment.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "health_score_configs")
public class HealthScoreConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64, unique = true)
    private String configKey = "default";

    @Column(name = "metric_weight")
    private Double metricWeight = 0.35;

    @Column(name = "fault_weight")
    private Double faultWeight = 0.30;

    @Column(name = "maintenance_weight")
    private Double maintenanceWeight = 0.20;

    @Column(name = "inspection_weight")
    private Double inspectionWeight = 0.15;

    @Column(name = "running_hours_normal_max")
    private Double runningHoursNormalMax = 8000.0;

    @Column(name = "load_normal_min")
    private Double loadNormalMin = 20.0;

    @Column(name = "load_normal_max")
    private Double loadNormalMax = 85.0;

    @Column(name = "temperature_normal_max")
    private Double temperatureNormalMax = 70.0;

    @Column(name = "temperature_warning_max")
    private Double temperatureWarningMax = 85.0;

    @Column(name = "vibration_normal_max")
    private Double vibrationNormalMax = 4.5;

    @Column(name = "vibration_warning_max")
    private Double vibrationWarningMax = 7.1;

    @Column(name = "maintenance_interval_days")
    private Integer maintenanceIntervalDays = 90;

    @Column(name = "maintenance_warning_days")
    private Integer maintenanceWarningDays = 120;

    @Column(name = "fault_window_days")
    private Integer faultWindowDays = 180;

    @Column(name = "fault_normal_max")
    private Integer faultNormalMax = 1;

    @Column(name = "risk_threshold_good")
    private Double riskThresholdGood = 80.0;

    @Column(name = "risk_threshold_warning")
    private Double riskThresholdWarning = 60.0;

    @Column(name = "risk_threshold_critical")
    private Double riskThresholdCritical = 40.0;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }
    public Double getMetricWeight() { return metricWeight; }
    public void setMetricWeight(Double metricWeight) { this.metricWeight = metricWeight; }
    public Double getFaultWeight() { return faultWeight; }
    public void setFaultWeight(Double faultWeight) { this.faultWeight = faultWeight; }
    public Double getMaintenanceWeight() { return maintenanceWeight; }
    public void setMaintenanceWeight(Double maintenanceWeight) { this.maintenanceWeight = maintenanceWeight; }
    public Double getInspectionWeight() { return inspectionWeight; }
    public void setInspectionWeight(Double inspectionWeight) { this.inspectionWeight = inspectionWeight; }
    public Double getRunningHoursNormalMax() { return runningHoursNormalMax; }
    public void setRunningHoursNormalMax(Double runningHoursNormalMax) { this.runningHoursNormalMax = runningHoursNormalMax; }
    public Double getLoadNormalMin() { return loadNormalMin; }
    public void setLoadNormalMin(Double loadNormalMin) { this.loadNormalMin = loadNormalMin; }
    public Double getLoadNormalMax() { return loadNormalMax; }
    public void setLoadNormalMax(Double loadNormalMax) { this.loadNormalMax = loadNormalMax; }
    public Double getTemperatureNormalMax() { return temperatureNormalMax; }
    public void setTemperatureNormalMax(Double temperatureNormalMax) { this.temperatureNormalMax = temperatureNormalMax; }
    public Double getTemperatureWarningMax() { return temperatureWarningMax; }
    public void setTemperatureWarningMax(Double temperatureWarningMax) { this.temperatureWarningMax = temperatureWarningMax; }
    public Double getVibrationNormalMax() { return vibrationNormalMax; }
    public void setVibrationNormalMax(Double vibrationNormalMax) { this.vibrationNormalMax = vibrationNormalMax; }
    public Double getVibrationWarningMax() { return vibrationWarningMax; }
    public void setVibrationWarningMax(Double vibrationWarningMax) { this.vibrationWarningMax = vibrationWarningMax; }
    public Integer getMaintenanceIntervalDays() { return maintenanceIntervalDays; }
    public void setMaintenanceIntervalDays(Integer maintenanceIntervalDays) { this.maintenanceIntervalDays = maintenanceIntervalDays; }
    public Integer getMaintenanceWarningDays() { return maintenanceWarningDays; }
    public void setMaintenanceWarningDays(Integer maintenanceWarningDays) { this.maintenanceWarningDays = maintenanceWarningDays; }
    public Integer getFaultWindowDays() { return faultWindowDays; }
    public void setFaultWindowDays(Integer faultWindowDays) { this.faultWindowDays = faultWindowDays; }
    public Integer getFaultNormalMax() { return faultNormalMax; }
    public void setFaultNormalMax(Integer faultNormalMax) { this.faultNormalMax = faultNormalMax; }
    public Double getRiskThresholdGood() { return riskThresholdGood; }
    public void setRiskThresholdGood(Double riskThresholdGood) { this.riskThresholdGood = riskThresholdGood; }
    public Double getRiskThresholdWarning() { return riskThresholdWarning; }
    public void setRiskThresholdWarning(Double riskThresholdWarning) { this.riskThresholdWarning = riskThresholdWarning; }
    public Double getRiskThresholdCritical() { return riskThresholdCritical; }
    public void setRiskThresholdCritical(Double riskThresholdCritical) { this.riskThresholdCritical = riskThresholdCritical; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
