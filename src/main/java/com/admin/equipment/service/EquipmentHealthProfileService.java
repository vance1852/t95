package com.admin.equipment.service;

import com.admin.equipment.model.*;
import com.admin.equipment.repo.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class EquipmentHealthProfileService {

    private final EquipmentRepository equipmentRepo;
    private final EquipmentRuntimeDataRepository runtimeRepo;
    private final WorkOrderRepository workOrderRepo;
    private final InspectionRecordRepository inspectionRepo;
    private final HealthScoreSnapshotRepository snapshotRepo;
    private final PredictiveMaintenanceOrderRepository predictionRepo;
    private final PredictionFeedbackRepository feedbackRepo;

    public EquipmentHealthProfileService(EquipmentRepository equipmentRepo,
                                         EquipmentRuntimeDataRepository runtimeRepo,
                                         WorkOrderRepository workOrderRepo,
                                         InspectionRecordRepository inspectionRepo,
                                         HealthScoreSnapshotRepository snapshotRepo,
                                         PredictiveMaintenanceOrderRepository predictionRepo,
                                         PredictionFeedbackRepository feedbackRepo) {
        this.equipmentRepo = equipmentRepo;
        this.runtimeRepo = runtimeRepo;
        this.workOrderRepo = workOrderRepo;
        this.inspectionRepo = inspectionRepo;
        this.snapshotRepo = snapshotRepo;
        this.predictionRepo = predictionRepo;
        this.feedbackRepo = feedbackRepo;
    }

    public Map<String, Object> getHealthProfile(Long equipmentId) {
        Equipment eq = equipmentRepo.findById(equipmentId).orElse(null);
        if (eq == null) return Collections.emptyMap();

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("equipment", eq);

        HealthScoreSnapshot latestScore = snapshotRepo.findLatestByEquipmentId(equipmentId).orElse(null);
        profile.put("latestHealthScore", latestScore);

        LocalDateTime dayAgo = LocalDateTime.now().minusDays(1);
        List<EquipmentRuntimeData> latestRuntime = runtimeRepo.findLatestByEquipmentId(equipmentId, 20);
        if (!latestRuntime.isEmpty()) {
            Map<String, Object> metrics = new LinkedHashMap<>();
            EquipmentRuntimeData d = latestRuntime.get(0);
            metrics.put("latestRecordedAt", d.getRecordedAt());
            metrics.put("runningHours", d.getRunningHours());
            metrics.put("temperatureC", d.getTemperatureC());
            metrics.put("vibrationMmS", d.getVibrationMmS());
            metrics.put("loadPercent", d.getLoadPercent());
            metrics.put("pressureMPa", d.getPressureMPa());
            metrics.put("currentAmp", d.getCurrentAmp());
            metrics.put("rpm", d.getRpm());

            if (latestRuntime.size() > 5) {
                double[] temps = latestRuntime.stream().filter(r -> r.getTemperatureC() != null)
                        .mapToDouble(EquipmentRuntimeData::getTemperatureC).toArray();
                double[] vibs = latestRuntime.stream().filter(r -> r.getVibrationMmS() != null)
                        .mapToDouble(EquipmentRuntimeData::getVibrationMmS).toArray();
                double[] loads = latestRuntime.stream().filter(r -> r.getLoadPercent() != null)
                        .mapToDouble(EquipmentRuntimeData::getLoadPercent).toArray();
                if (temps.length > 0) metrics.put("avgTemperature24h", avg(temps));
                if (vibs.length > 0) metrics.put("avgVibration24h", avg(vibs));
                if (loads.length > 0) metrics.put("avgLoad24h", avg(loads));
            }
            profile.put("currentMetrics", metrics);
        }

        LocalDateTime halfYearAgo = LocalDateTime.now().minusDays(180);
        List<WorkOrder> repairs = workOrderRepo.findRecentRepairOrders(equipmentId, halfYearAgo);
        List<WorkOrder> maintenances = workOrderRepo.findCompletedMaintenanceOrders(equipmentId);
        profile.put("recentRepairs", repairs);
        profile.put("repairCount180d", repairs.size());
        profile.put("lastMaintenance", maintenances.isEmpty() ? null : maintenances.get(0));

        List<InspectionRecord> inspections = inspectionRepo.findByEquipmentIdOrderByInspectedAtDesc(equipmentId);
        profile.put("recentInspections", inspections.stream().limit(10).toList());
        profile.put("inspectionCount", inspections.size());
        profile.put("anomalyInspectionCount", inspections.stream().filter(i -> Boolean.TRUE.equals(i.getHasAnomaly())).count());

        LocalDateTime monthAgo = LocalDateTime.now().minusDays(30);
        List<PredictiveMaintenanceOrder> predictions = predictionRepo.findByEquipmentIdOrderByGeneratedAtDesc(equipmentId);
        profile.put("predictions", predictions.stream().limit(10).toList());
        profile.put("pendingPredictions", predictions.stream().filter(p -> "pending".equals(p.getStatus())).count());

        return profile;
    }

    public Map<String, Object> getPredictionAccuracyOverview(int days) {
        LocalDateTime from = LocalDateTime.now().minusDays(days);
        Map<String, Object> result = new LinkedHashMap<>();

        Double avgAccuracy = feedbackRepo.findAverageAccuracySince(from);
        long correct = feedbackRepo.countCorrectPredictions(from);
        long total = feedbackRepo.countTotalPredictions(from);

        result.put("periodDays", days);
        result.put("totalPredictions", total);
        result.put("correctPredictions", correct);
        result.put("averageAccuracy", avgAccuracy == null ? 0 : round2(avgAccuracy));
        result.put("hitRate", total == 0 ? 0 : round2(correct * 100.0 / total));

        List<PredictionFeedback> recent = feedbackRepo.findAll().stream()
                .filter(f -> f.getCreatedAt() != null && f.getCreatedAt().isAfter(from))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(50)
                .toList();
        result.put("recentFeedbacks", recent);

        return result;
    }

    public PredictionFeedback recordPredictionFeedback(Long predictionId, boolean faultOccurred,
                                                       LocalDateTime actualDate, String remark) {
        PredictiveMaintenanceOrder pred = predictionRepo.findById(predictionId).orElse(null);
        if (pred == null) return null;

        PredictionFeedback fb = new PredictionFeedback();
        fb.setEquipmentId(pred.getEquipmentId());
        fb.setPredictionId(predictionId);
        fb.setPredictedDate(pred.getPredictedFaultDate());
        fb.setActualDate(actualDate);
        fb.setFaultOccurred(faultOccurred);
        fb.setRemark(remark);

        if (pred.getPredictedFaultDate() != null && actualDate != null) {
            long days = Math.abs(java.time.Duration.between(pred.getPredictedFaultDate(), actualDate).toDays());
            fb.setDaysDeviation((int) days);
            double accuracy = Math.max(0, 100 - days * 3.33);
            fb.setAccuracyScore(round2(accuracy));
        } else if (faultOccurred) {
            fb.setAccuracyScore(80.0);
        } else {
            fb.setAccuracyScore(0.0);
        }

        pred.setActualFaultOccurred(faultOccurred);
        pred.setPredictionAccuracy(fb.getAccuracyScore());
        pred.setStatus("reviewed");
        pred.setResolvedAt(LocalDateTime.now());
        predictionRepo.save(pred);

        return feedbackRepo.save(fb);
    }

    private double avg(double[] arr) {
        double s = 0;
        for (double v : arr) s += v;
        return Math.round(s / arr.length * 100.0) / 100.0;
    }

    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }
}
