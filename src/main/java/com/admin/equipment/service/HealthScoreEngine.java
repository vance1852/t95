package com.admin.equipment.service;

import com.admin.equipment.model.*;
import com.admin.equipment.repo.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class HealthScoreEngine {

    private final HealthScoreConfigRepository configRepo;
    private final EquipmentRuntimeDataRepository runtimeRepo;
    private final WorkOrderRepository workOrderRepo;
    private final InspectionRecordRepository inspectionRepo;
    private final HealthScoreSnapshotRepository snapshotRepo;
    private final ObjectMapper objectMapper;

    public HealthScoreEngine(HealthScoreConfigRepository configRepo,
                             EquipmentRuntimeDataRepository runtimeRepo,
                             WorkOrderRepository workOrderRepo,
                             InspectionRecordRepository inspectionRepo,
                             HealthScoreSnapshotRepository snapshotRepo,
                             ObjectMapper objectMapper) {
        this.configRepo = configRepo;
        this.runtimeRepo = runtimeRepo;
        this.workOrderRepo = workOrderRepo;
        this.inspectionRepo = inspectionRepo;
        this.snapshotRepo = snapshotRepo;
        this.objectMapper = objectMapper;
    }

    public static class ScoreResult {
        public double totalScore;
        public double metricScore;
        public double faultScore;
        public double maintenanceScore;
        public double inspectionScore;
        public String riskLevel;
        public List<Factor> negativeFactors = new ArrayList<>();
        public List<Factor> positiveFactors = new ArrayList<>();
    }

    public static class Factor {
        public String name;
        public String description;
        public double impact;
        public Factor(String name, String description, double impact) {
            this.name = name;
            this.description = description;
            this.impact = impact;
        }
    }

    public ScoreResult calculateHealthScore(Long equipmentId) {
        HealthScoreConfig config = configRepo.findByConfigKey("default")
                .orElseGet(() -> {
                    HealthScoreConfig c = new HealthScoreConfig();
                    c.setConfigKey("default");
                    return c;
                });

        ScoreResult result = new ScoreResult();
        double metricWeight = normalizeWeight(config.getMetricWeight());
        double faultWeight = normalizeWeight(config.getFaultWeight());
        double maintenanceWeight = normalizeWeight(config.getMaintenanceWeight());
        double inspectionWeight = normalizeWeight(config.getInspectionWeight());

        double totalWeight = metricWeight + faultWeight + maintenanceWeight + inspectionWeight;
        if (totalWeight <= 0) totalWeight = 1.0;

        result.metricScore = calculateMetricScore(equipmentId, config, result);
        result.faultScore = calculateFaultScore(equipmentId, config, result);
        result.maintenanceScore = calculateMaintenanceScore(equipmentId, config, result);
        result.inspectionScore = calculateInspectionScore(equipmentId, config, result);

        result.totalScore = (result.metricScore * metricWeight +
                result.faultScore * faultWeight +
                result.maintenanceScore * maintenanceWeight +
                result.inspectionScore * inspectionWeight) / totalWeight;

        result.totalScore = Math.max(0, Math.min(100, result.totalScore));
        result.riskLevel = determineRiskLevel(result.totalScore, config);
        result.negativeFactors.sort((a, b) -> Double.compare(b.impact, a.impact));

        return result;
    }

    public HealthScoreSnapshot calculateAndSaveSnapshot(Long equipmentId) {
        ScoreResult result = calculateHealthScore(equipmentId);
        HealthScoreConfig config = configRepo.findByConfigKey("default")
                .orElseGet(HealthScoreConfig::new);

        HealthScoreSnapshot snapshot = new HealthScoreSnapshot();
        snapshot.setEquipmentId(equipmentId);
        snapshot.setSnapshotTime(LocalDateTime.now());
        snapshot.setTotalScore(round2(result.totalScore));
        snapshot.setMetricScore(round2(result.metricScore));
        snapshot.setFaultScore(round2(result.faultScore));
        snapshot.setMaintenanceScore(round2(result.maintenanceScore));
        snapshot.setInspectionScore(round2(result.inspectionScore));
        snapshot.setMetricWeight(config.getMetricWeight());
        snapshot.setFaultWeight(config.getFaultWeight());
        snapshot.setMaintenanceWeight(config.getMaintenanceWeight());
        snapshot.setInspectionWeight(config.getInspectionWeight());
        snapshot.setRiskLevel(result.riskLevel);

        try {
            Map<String, Object> factors = new LinkedHashMap<>();
            List<Map<String, Object>> negList = new ArrayList<>();
            for (Factor f : result.negativeFactors) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", f.name);
                m.put("description", f.description);
                m.put("impact", round2(f.impact));
                negList.add(m);
            }
            factors.put("negative", negList);
            List<Map<String, Object>> posList = new ArrayList<>();
            for (Factor f : result.positiveFactors) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", f.name);
                m.put("description", f.description);
                m.put("impact", round2(f.impact));
                posList.add(m);
            }
            factors.put("positive", posList);
            snapshot.setFactors(objectMapper.writeValueAsString(factors));
        } catch (JsonProcessingException e) {
            snapshot.setFactors("{}");
        }

        return snapshotRepo.save(snapshot);
    }

    private double calculateMetricScore(Long equipmentId, HealthScoreConfig config, ScoreResult result) {
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        List<EquipmentRuntimeData> recentData = runtimeRepo.findByEquipmentIdAndRecordedAtAfter(equipmentId, from);

        if (recentData.isEmpty()) {
            result.negativeFactors.add(new Factor("运行数据缺失", "最近7天无运行指标数据，无法评估运行状态", 15.0));
            return 70.0;
        }

        double score = 100.0;
        int dataPoints = 0;

        double avgTemp = 0, avgVibration = 0, avgLoad = 0, maxTemp = 0, maxVibration = 0;
        int tempCount = 0, vibCount = 0, loadCount = 0;

        for (EquipmentRuntimeData d : recentData) {
            dataPoints++;
            if (d.getTemperatureC() != null) {
                avgTemp += d.getTemperatureC();
                maxTemp = Math.max(maxTemp, d.getTemperatureC());
                tempCount++;
            }
            if (d.getVibrationMmS() != null) {
                avgVibration += d.getVibrationMmS();
                maxVibration = Math.max(maxVibration, d.getVibrationMmS());
                vibCount++;
            }
            if (d.getLoadPercent() != null) {
                avgLoad += d.getLoadPercent();
                loadCount++;
            }
        }

        if (tempCount > 0) {
            avgTemp /= tempCount;
            double tempPenalty = 0;
            if (avgTemp > config.getTemperatureNormalMax()) {
                double excess = (avgTemp - config.getTemperatureNormalMax()) /
                        Math.max(1, config.getTemperatureWarningMax() - config.getTemperatureNormalMax());
                tempPenalty = Math.min(25, excess * 25);
                result.negativeFactors.add(new Factor("温度偏高",
                        String.format("近7天平均温度%.1f°C，超过正常阈值%.1f°C", avgTemp, config.getTemperatureNormalMax()),
                        tempPenalty));
            } else if (avgTemp < config.getTemperatureNormalMax() * 0.7) {
                result.positiveFactors.add(new Factor("温度良好",
                        String.format("近7天平均温度%.1f°C，运行稳定", avgTemp), 5.0));
            }
            if (maxTemp > config.getTemperatureWarningMax()) {
                double spikePenalty = Math.min(15, (maxTemp - config.getTemperatureWarningMax()) * 2);
                tempPenalty += spikePenalty;
                result.negativeFactors.add(new Factor("温度峰值告警",
                        String.format("曾出现%.1f°C高温峰值，超过告警阈值%.1f°C", maxTemp, config.getTemperatureWarningMax()),
                        spikePenalty));
            }
            score -= tempPenalty;
        }

        if (vibCount > 0) {
            avgVibration /= vibCount;
            double vibPenalty = 0;
            if (avgVibration > config.getVibrationNormalMax()) {
                double excess = (avgVibration - config.getVibrationNormalMax()) /
                        Math.max(0.1, config.getVibrationWarningMax() - config.getVibrationNormalMax());
                vibPenalty = Math.min(25, excess * 25);
                result.negativeFactors.add(new Factor("振动偏大",
                        String.format("近7天平均振动%.2fmm/s，超过正常阈值%.2fmm/s", avgVibration, config.getVibrationNormalMax()),
                        vibPenalty));
            } else if (avgVibration < config.getVibrationNormalMax() * 0.6) {
                result.positiveFactors.add(new Factor("振动良好",
                        String.format("近7天平均振动%.2fmm/s，机械状态稳定", avgVibration), 5.0));
            }
            if (maxVibration > config.getVibrationWarningMax()) {
                double spikePenalty = Math.min(15, (maxVibration - config.getVibrationWarningMax()) * 3);
                vibPenalty += spikePenalty;
                result.negativeFactors.add(new Factor("振动峰值告警",
                        String.format("曾出现%.2fmm/s振动峰值", maxVibration), spikePenalty));
            }
            score -= vibPenalty;
        }

        if (loadCount > 0) {
            avgLoad /= loadCount;
            if (avgLoad > config.getLoadNormalMax()) {
                double penalty = Math.min(15, (avgLoad - config.getLoadNormalMax()) * 0.8);
                score -= penalty;
                result.negativeFactors.add(new Factor("负载过高",
                        String.format("近7天平均负载%.1f%%，长期过载运行", avgLoad), penalty));
            } else if (avgLoad < config.getLoadNormalMin() && avgLoad > 0) {
                double penalty = Math.min(8, (config.getLoadNormalMin() - avgLoad) * 0.3);
                score -= penalty;
                result.negativeFactors.add(new Factor("负载偏低",
                        String.format("近7天平均负载%.1f%%，可能空转或低效运行", avgLoad), penalty));
            } else if (avgLoad >= config.getLoadNormalMin() && avgLoad <= config.getLoadNormalMax()) {
                result.positiveFactors.add(new Factor("负载合理",
                        String.format("近7天平均负载%.1f%%，处于最佳工作区间", avgLoad), 3.0));
            }
        }

        EquipmentRuntimeData latest = recentData.get(0);
        if (latest.getRunningHours() != null && latest.getRunningHours() > config.getRunningHoursNormalMax()) {
            double ratio = latest.getRunningHours() / config.getRunningHoursNormalMax();
            double penalty = Math.min(15, (ratio - 1) * 20);
            score -= penalty;
            result.negativeFactors.add(new Factor("累计运行时长偏长",
                    String.format("已运行%.0f小时，超过建议阈值%.0f小时", latest.getRunningHours(), config.getRunningHoursNormalMax()),
                    penalty));
        }

        if (dataPoints < 24) {
            double penalty = Math.min(10, (24 - dataPoints) * 0.3);
            score -= penalty;
            result.negativeFactors.add(new Factor("数据采样不足",
                    String.format("近7天仅%d条数据记录，采样稀疏", dataPoints), penalty));
        }

        return Math.max(0, Math.min(100, score));
    }

    private double calculateFaultScore(Long equipmentId, HealthScoreConfig config, ScoreResult result) {
        LocalDateTime from = LocalDateTime.now().minusDays(config.getFaultWindowDays());
        long faultCount = workOrderRepo.countRecentRepairs(equipmentId, from);

        double score = 100.0;
        if (faultCount > config.getFaultNormalMax()) {
            double excess = faultCount - config.getFaultNormalMax();
            double penalty = Math.min(60, excess * 20);
            score -= penalty;
            result.negativeFactors.add(new Factor("故障频次高",
                    String.format("近%d天发生%d次故障维修，超过正常阈值%d次",
                            config.getFaultWindowDays(), faultCount, config.getFaultNormalMax()),
                    penalty));
        } else if (faultCount == 0) {
            result.positiveFactors.add(new Factor("无故障记录",
                    String.format("近%d天无故障维修记录", config.getFaultWindowDays()), 8.0));
        } else {
            result.negativeFactors.add(new Factor("偶发故障",
                    String.format("近%d天有%d次故障维修", config.getFaultWindowDays(), faultCount),
                    faultCount * 10.0));
            score -= faultCount * 10.0;
        }

        return Math.max(0, Math.min(100, score));
    }

    private double calculateMaintenanceScore(Long equipmentId, HealthScoreConfig config, ScoreResult result) {
        List<WorkOrder> maintenances = workOrderRepo.findCompletedMaintenanceOrders(equipmentId);
        LocalDateTime lastMaintenance = null;
        if (!maintenances.isEmpty()) {
            lastMaintenance = maintenances.get(0).getClosedAt();
        }

        double score = 100.0;
        if (lastMaintenance == null) {
            score = 55.0;
            result.negativeFactors.add(new Factor("从未保养", "设备暂无保养记录，建议尽快安排首次保养", 45.0));
            return score;
        }

        long daysSince = Duration.between(lastMaintenance, LocalDateTime.now()).toDays();
        int interval = config.getMaintenanceIntervalDays();
        int warning = config.getMaintenanceWarningDays();

        if (daysSince > warning) {
            double penalty = Math.min(50, (daysSince - warning) * 1.2);
            score -= penalty;
            result.negativeFactors.add(new Factor("保养严重超期",
                    String.format("距上次保养已%d天，远超建议周期%d天", daysSince, interval), penalty));
        } else if (daysSince > interval) {
            double penalty = Math.min(25, (daysSince - interval) * 1.5);
            score -= penalty;
            result.negativeFactors.add(new Factor("保养超期",
                    String.format("距上次保养已%d天，超过建议周期%d天", daysSince, interval), penalty));
        } else if (daysSince < interval * 0.3) {
            result.positiveFactors.add(new Factor("近期已保养",
                    String.format("%d天前刚完成保养", daysSince), 5.0));
        }

        return Math.max(0, Math.min(100, score));
    }

    private double calculateInspectionScore(Long equipmentId, HealthScoreConfig config, ScoreResult result) {
        LocalDateTime from = LocalDateTime.now().minusDays(30);
        long anomalyCount = inspectionRepo.countRecentAnomalies(equipmentId, from);
        List<InspectionRecord> anomalies = inspectionRepo.findRecentAnomalies(equipmentId, from);

        double score = 100.0;
        if (anomalyCount == 0) {
            result.positiveFactors.add(new Factor("巡检无异常", "近30天巡检未发现异常项", 5.0));
            return score;
        }

        double penalty = 0;
        int highCount = 0, mediumCount = 0, lowCount = 0;
        for (InspectionRecord r : anomalies) {
            String level = r.getAnomalyLevel() == null ? "medium" : r.getAnomalyLevel();
            switch (level) {
                case "critical": case "high": highCount++; break;
                case "medium": mediumCount++; break;
                default: lowCount++;
            }
        }

        penalty = highCount * 18 + mediumCount * 10 + lowCount * 5;
        penalty = Math.min(60, penalty);
        score -= penalty;

        StringBuilder sb = new StringBuilder("近30天巡检发现");
        if (highCount > 0) sb.append(highCount).append("项严重异常 ");
        if (mediumCount > 0) sb.append(mediumCount).append("项中度异常 ");
        if (lowCount > 0) sb.append(lowCount).append("项轻微异常");
        result.negativeFactors.add(new Factor("巡检异常", sb.toString(), penalty));

        return Math.max(0, Math.min(100, score));
    }

    private double normalizeWeight(Double w) {
        if (w == null) return 0;
        return Math.max(0, w);
    }

    private String determineRiskLevel(double score, HealthScoreConfig config) {
        if (score >= config.getRiskThresholdGood()) return "good";
        if (score >= config.getRiskThresholdWarning()) return "warning";
        if (score >= config.getRiskThresholdCritical()) return "critical";
        return "danger";
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
