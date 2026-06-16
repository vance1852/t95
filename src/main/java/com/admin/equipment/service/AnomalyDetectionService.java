package com.admin.equipment.service;

import com.admin.equipment.model.AnomalyAlert;
import com.admin.equipment.model.EquipmentRuntimeData;
import com.admin.equipment.model.HealthScoreConfig;
import com.admin.equipment.model.HealthScoreSnapshot;
import com.admin.equipment.repo.AnomalyAlertRepository;
import com.admin.equipment.repo.EquipmentRuntimeDataRepository;
import com.admin.equipment.repo.HealthScoreConfigRepository;
import com.admin.equipment.repo.HealthScoreSnapshotRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AnomalyDetectionService {

    private final AnomalyAlertRepository alertRepo;
    private final EquipmentRuntimeDataRepository runtimeRepo;
    private final HealthScoreSnapshotRepository snapshotRepo;
    private final HealthScoreConfigRepository configRepo;

    public AnomalyDetectionService(AnomalyAlertRepository alertRepo,
                                   EquipmentRuntimeDataRepository runtimeRepo,
                                   HealthScoreSnapshotRepository snapshotRepo,
                                   HealthScoreConfigRepository configRepo) {
        this.alertRepo = alertRepo;
        this.runtimeRepo = runtimeRepo;
        this.snapshotRepo = snapshotRepo;
        this.configRepo = configRepo;
    }

    public List<AnomalyAlert> detectMetricAnomalies(Long equipmentId) {
        HealthScoreConfig config = configRepo.findByConfigKey("default")
                .orElseGet(HealthScoreConfig::new);
        List<EquipmentRuntimeData> recent = runtimeRepo.findLatestByEquipmentId(equipmentId, 50);
        if (recent.isEmpty()) return Collections.emptyList();

        List<AnomalyAlert> alerts = new ArrayList<>();
        LocalDateTime dayAgo = LocalDateTime.now().minusDays(1);

        double[] temps = recent.stream().filter(d -> d.getTemperatureC() != null)
                .mapToDouble(EquipmentRuntimeData::getTemperatureC).toArray();
        double[] vibs = recent.stream().filter(d -> d.getVibrationMmS() != null)
                .mapToDouble(EquipmentRuntimeData::getVibrationMmS).toArray();
        double[] loads = recent.stream().filter(d -> d.getLoadPercent() != null)
                .mapToDouble(EquipmentRuntimeData::getLoadPercent).toArray();

        if (temps.length > 5) {
            double mean = mean(temps);
            double std = stddev(temps, mean);
            double latest = temps[0];
            if (latest > config.getTemperatureWarningMax()) {
                alerts.add(createAlert(equipmentId, "metric", "temperature_c", latest,
                        config.getTemperatureWarningMax(), "critical",
                        "温度严重超阈值",
                        String.format("当前温度%.1f°C超过告警阈值%.1f°C", latest, config.getTemperatureWarningMax())));
            } else if (std > 0 && latest > mean + 3 * std) {
                alerts.add(createAlert(equipmentId, "metric", "temperature_c", latest,
                        mean + 3 * std, "warning",
                        "温度统计异常（3σ）",
                        String.format("当前温度%.1f°C，均值%.1f°C，标准差%.1f°C，突增超过3σ", latest, mean, std)));
            } else if (latest > config.getTemperatureNormalMax() &&
                    alertRepo.findRecentMetricAlerts(equipmentId, "temperature_c", dayAgo).isEmpty()) {
                alerts.add(createAlert(equipmentId, "metric", "temperature_c", latest,
                        config.getTemperatureNormalMax(), "info",
                        "温度偏高",
                        String.format("当前温度%.1f°C超过正常阈值%.1f°C", latest, config.getTemperatureNormalMax())));
            }
        }

        if (vibs.length > 5) {
            double mean = mean(vibs);
            double std = stddev(vibs, mean);
            double latest = vibs[0];
            if (latest > config.getVibrationWarningMax()) {
                alerts.add(createAlert(equipmentId, "metric", "vibration_mm_s", latest,
                        config.getVibrationWarningMax(), "critical",
                        "振动严重超阈值",
                        String.format("当前振动%.2fmm/s超过告警阈值%.2fmm/s", latest, config.getVibrationWarningMax())));
            } else if (std > 0 && latest > mean + 3 * std) {
                alerts.add(createAlert(equipmentId, "metric", "vibration_mm_s", latest,
                        mean + 3 * std, "warning",
                        "振动统计异常（3σ）",
                        String.format("当前振动%.2fmm/s，均值%.2f，突增超过3σ", latest, mean)));
            }
        }

        if (loads.length > 5) {
            double latest = loads[loads.length - 1];
            if (latest > 95) {
                alerts.add(createAlert(equipmentId, "metric", "load_percent", latest,
                        95.0, "warning",
                        "接近满负载",
                        String.format("当前负载%.1f%%，长期高负载运行将加速设备劣化", latest)));
            }
        }

        List<AnomalyAlert> saved = new ArrayList<>();
        for (AnomalyAlert a : alerts) {
            boolean isCritical = "critical".equals(a.getSeverity());
            if (isCritical || alertRepo.findRecentMetricAlerts(equipmentId, a.getMetricName(), dayAgo).isEmpty()) {
                saved.add(alertRepo.save(a));
            }
        }
        return alerts;
    }

    public List<AnomalyAlert> detectHealthScoreDrop(Long equipmentId) {
        List<HealthScoreSnapshot> snapshots = snapshotRepo.findLatestByEquipmentId(equipmentId)
                .map(List::of).orElse(Collections.emptyList());
        if (snapshots.isEmpty()) return Collections.emptyList();

        HealthScoreSnapshot latest = snapshots.get(0);
        List<AnomalyAlert> alerts = new ArrayList<>();
        LocalDateTime dayAgo = LocalDateTime.now().minusDays(1);

        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        List<HealthScoreSnapshot> weekly = snapshotRepo
                .findByEquipmentIdAndSnapshotTimeAfter(equipmentId, weekAgo);

        if (weekly.size() >= 2) {
            double drop = weekly.get(0).getTotalScore() - latest.getTotalScore();
            if (drop >= 15) {
                alerts.add(createHealthDropAlert(equipmentId, drop, latest.getTotalScore(),
                        weekly.get(0).getTotalScore(), "critical"));
            } else if (drop >= 8) {
                alerts.add(createHealthDropAlert(equipmentId, drop, latest.getTotalScore(),
                        weekly.get(0).getTotalScore(), "warning"));
            }
        }

        List<AnomalyAlert> saved = new ArrayList<>();
        for (AnomalyAlert a : alerts) {
            if (alertRepo.findRecentMetricAlerts(equipmentId, "health_score", dayAgo).isEmpty()) {
                saved.add(alertRepo.save(a));
            }
        }
        return saved;
    }

    public List<AnomalyAlert> getActiveAlerts() {
        return alertRepo.findByStatusOrderByDetectedAtDesc("active");
    }

    public List<AnomalyAlert> getRecentAlerts(int days) {
        return alertRepo.findRecentAlerts(LocalDateTime.now().minusDays(days));
    }

    public AnomalyAlert acknowledgeAlert(Long alertId, String acknowledgedBy) {
        AnomalyAlert a = alertRepo.findById(alertId).orElse(null);
        if (a == null) return null;
        a.setStatus("acknowledged");
        a.setAcknowledgedAt(LocalDateTime.now());
        a.setAcknowledgedBy(acknowledgedBy == null ? "" : acknowledgedBy);
        return alertRepo.save(a);
    }

    private AnomalyAlert createAlert(Long eqId, String type, String metric, Double value,
                                     Double threshold, String severity, String title, String desc) {
        AnomalyAlert a = new AnomalyAlert();
        a.setEquipmentId(eqId);
        a.setAlertType(type);
        a.setMetricName(metric);
        a.setMetricValue(round2(value));
        a.setThresholdValue(round2(threshold));
        a.setSeverity(severity);
        a.setTitle(title);
        a.setDescription(desc);
        a.setStatus("active");
        a.setDetectedAt(LocalDateTime.now());
        return a;
    }

    private AnomalyAlert createHealthDropAlert(Long eqId, double drop, double current, double before, String severity) {
        AnomalyAlert a = new AnomalyAlert();
        a.setEquipmentId(eqId);
        a.setAlertType("health_score");
        a.setMetricName("health_score");
        a.setHealthScoreDrop(round2(drop));
        a.setSeverity(severity);
        a.setTitle(String.format("健康分骤降%.1f分", drop));
        a.setDescription(String.format("7天内健康分从%.1f降至%.1f，降幅%.1f分，需关注设备状态", before, current, drop));
        a.setStatus("active");
        a.setDetectedAt(LocalDateTime.now());
        return a;
    }

    private double mean(double[] arr) {
        double s = 0;
        for (double v : arr) s += v;
        return s / arr.length;
    }

    private double stddev(double[] arr, double mean) {
        double s = 0;
        for (double v : arr) s += (v - mean) * (v - mean);
        return Math.sqrt(s / arr.length);
    }

    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }
}
