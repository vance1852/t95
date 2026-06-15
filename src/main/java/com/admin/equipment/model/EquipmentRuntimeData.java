package com.admin.equipment.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "equipment_runtime_data", indexes = {
    @Index(name = "idx_runtime_equipment_time", columnList = "equipment_id, recorded_at")
})
public class EquipmentRuntimeData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Column(name = "running_hours")
    private Double runningHours;

    @Column(name = "load_percent")
    private Double loadPercent;

    @Column(name = "temperature_c")
    private Double temperatureC;

    @Column(name = "vibration_mm_s")
    private Double vibrationMmS;

    @Column(name = "pressure_mpa")
    private Double pressureMPa;

    @Column(name = "current_amp")
    private Double currentAmp;

    @Column(name = "rpm")
    private Double rpm;

    @Column(length = 512)
    private String remark = "";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
    public Double getRunningHours() { return runningHours; }
    public void setRunningHours(Double runningHours) { this.runningHours = runningHours; }
    public Double getLoadPercent() { return loadPercent; }
    public void setLoadPercent(Double loadPercent) { this.loadPercent = loadPercent; }
    public Double getTemperatureC() { return temperatureC; }
    public void setTemperatureC(Double temperatureC) { this.temperatureC = temperatureC; }
    public Double getVibrationMmS() { return vibrationMmS; }
    public void setVibrationMmS(Double vibrationMmS) { this.vibrationMmS = vibrationMmS; }
    public Double getPressureMPa() { return pressureMPa; }
    public void setPressureMPa(Double pressureMPa) { this.pressureMPa = pressureMPa; }
    public Double getCurrentAmp() { return currentAmp; }
    public void setCurrentAmp(Double currentAmp) { this.currentAmp = currentAmp; }
    public Double getRpm() { return rpm; }
    public void setRpm(Double rpm) { this.rpm = rpm; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
