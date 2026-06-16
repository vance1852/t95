package com.admin.equipment.service;

import com.admin.equipment.model.*;
import com.admin.equipment.repo.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class PredictiveMaintenanceService {

    private final HealthScoreSnapshotRepository snapshotRepo;
    private final EquipmentRepository equipmentRepo;
    private final HealthScoreConfigRepository configRepo;
    private final PredictiveMaintenanceOrderRepository predictionRepo;
    private final WorkOrderRepository workOrderRepo;
    private final ObjectMapper objectMapper;

    public PredictiveMaintenanceService(HealthScoreSnapshotRepository snapshotRepo,
                                        EquipmentRepository equipmentRepo,
                                        HealthScoreConfigRepository configRepo,
                                        PredictiveMaintenanceOrderRepository predictionRepo,
                                        WorkOrderRepository workOrderRepo,
                                        ObjectMapper objectMapper) {
        this.snapshotRepo = snapshotRepo;
        this.equipmentRepo = equipmentRepo;
        this.configRepo = configRepo;
        this.predictionRepo = predictionRepo;
        this.workOrderRepo = workOrderRepo;
        this.objectMapper = objectMapper;
    }

    public static class PredictionResult {
        public Integer remainingHealthDays;
        public LocalDateTime predictedFaultDate;
        public Double degradationRate;
        public Double currentScore;
        public boolean predictable;
        public String reason;
        public List<Map<String, Object>> trendPoints;
    }

    public PredictionResult predictRemainingHealth(Long equipmentId) {
        PredictionResult result = new PredictionResult();
        HealthScoreConfig config = configRepo.findByConfigKey("default")
                .orElseGet(HealthScoreConfig::new);

        LocalDateTime from = LocalDateTime.now().minusDays(30);
        List<HealthScoreSnapshot> snapshots = snapshotRepo
                .findByEquipmentIdAndSnapshotTimeAfter(equipmentId, from);

        result.trendPoints = new ArrayList<>();
        for (HealthScoreSnapshot s : snapshots) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("time", s.getSnapshotTime());
            p.put("score", s.getTotalScore());
            result.trendPoints.add(p);
        }

        if (snapshots.size() < 3) {
            result.predictable = false;
            result.reason = "健康分历史数据不足，至少需要3个快照";
            result.currentScore = snapshots.isEmpty() ? null : snapshots.get(snapshots.size() - 1).getTotalScore();
            return result;
        }

        double threshold = config.getRiskThresholdWarning();

        double[] scores = snapshots.stream().mapToDouble(HealthScoreSnapshot::getTotalScore).toArray();
        long[] timestamps = new long[scores.length];
        LocalDateTime t0 = snapshots.get(0).getSnapshotTime();
        for (int i = 0; i < snapshots.size(); i++) {
            timestamps[i] = ChronoUnit.HOURS.between(t0, snapshots.get(i).getSnapshotTime());
        }

        double slope = linearRegressionSlope(timestamps, scores);
        result.degradationRate = round4(slope);
        result.currentScore = round2(scores[scores.length - 1]);

        if (slope >= 0) {
            result.predictable = false;
            result.reason = "健康分呈稳定或上升趋势，暂无劣化迹象";
            return result;
        }

        double hoursToThreshold = (threshold - scores[scores.length - 1]) / slope;
        if (hoursToThreshold < 0) {
            result.remainingHealthDays = 0;
            result.predictedFaultDate = LocalDateTime.now();
            result.predictable = true;
            result.reason = "当前健康分已低于风险阈值";
            return result;
        }

        if (hoursToThreshold > 24 * 365) {
            result.predictable = false;
            result.reason = "预测周期超过1年，劣化速率极慢，预测参考价值低";
            return result;
        }

        result.remainingHealthDays = (int) Math.round(hoursToThreshold / 24.0);
        result.predictedFaultDate = LocalDateTime.now().plusDays(result.remainingHealthDays);
        result.predictable = true;
        result.reason = String.format("按当前劣化速率(%.4f分/小时)，预计%d天后跌至风险阈值%.0f分",
                slope, result.remainingHealthDays, threshold);

        return result;
    }

    @Transactional
    public PredictiveMaintenanceOrder generatePredictiveOrder(Long equipmentId) {
        Equipment eq = equipmentRepo.findById(equipmentId).orElse(null);
        if (eq == null) return null;

        List<PredictiveMaintenanceOrder> existing = predictionRepo.findByEquipmentIdOrderByGeneratedAtDesc(equipmentId);
        boolean hasPending = existing.stream().anyMatch(p -> "pending".equals(p.getStatus()));
        if (hasPending) return null;

        PredictionResult pred = predictRemainingHealth(equipmentId);
        if (!pred.predictable || pred.remainingHealthDays == null || pred.remainingHealthDays > 60) {
            return null;
        }

        PredictiveMaintenanceOrder order = new PredictiveMaintenanceOrder();
        order.setEquipmentId(equipmentId);
        order.setTitle(String.format("[预测保养] %s 预计%d天后达到风险阈值", eq.getName(), pred.remainingHealthDays));
        order.setDescription(String.format("设备 %s(%s) 当前健康分%.1f，按劣化速率%.4f分/小时，预计约%d天后(约%s)跌至风险阈值。建议提前安排预测性保养。",
                eq.getName(), eq.getCode(), pred.currentScore, pred.degradationRate,
                pred.remainingHealthDays,
                pred.predictedFaultDate != null ? pred.predictedFaultDate.toLocalDate() : "未知"));
        order.setPredictedFaultDate(pred.predictedFaultDate);
        order.setRemainingHealthDays(pred.remainingHealthDays);
        order.setCurrentHealthScore(pred.currentScore);
        order.setRiskLevel(determineRiskByDays(pred.remainingHealthDays));
        order.setPriority(determinePriorityByDays(pred.remainingHealthDays));
        order.setSuggestedActions(generateSuggestedActions(eq, pred));

        try {
            order.setHealthScoreTrend(objectMapper.writeValueAsString(pred.trendPoints));
        } catch (JsonProcessingException e) {
            order.setHealthScoreTrend("[]");
        }

        return predictionRepo.save(order);
    }

    @Transactional
    public WorkOrder convertPredictionToWorkOrder(Long predictionId, String assignee) {
        PredictiveMaintenanceOrder pred = predictionRepo.findById(predictionId).orElse(null);
        if (pred == null) return null;

        WorkOrder wo = new WorkOrder();
        wo.setEquipmentId(pred.getEquipmentId());
        wo.setTitle(pred.getTitle().replace("[预测保养]", "[预测保养-已排程]"));
        wo.setType("maintenance");
        wo.setPriority(pred.getPriority());
        wo.setDescription(pred.getDescription() + "\n\n建议措施:\n" + pred.getSuggestedActions());
        wo.setAssignee(assignee == null ? "" : assignee);
        wo.setStatus("open");
        WorkOrder saved = workOrderRepo.save(wo);

        pred.setWorkOrderId(saved.getId());
        pred.setStatus("converted");
        predictionRepo.save(pred);

        return saved;
    }

    public List<PredictiveMaintenanceOrder> getPendingPredictions(Integer maxDays) {
        if (maxDays != null) {
            return predictionRepo.findUrgentPredictions(maxDays);
        }
        return predictionRepo.findByStatusOrderByGeneratedAtDesc("pending");
    }

    private double linearRegressionSlope(long[] x, double[] y) {
        int n = x.length;
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            sumX += x[i];
            sumY += y[i];
            sumXY += x[i] * y[i];
            sumX2 += x[i] * x[i];
        }
        double denom = n * sumX2 - sumX * sumX;
        if (denom == 0) return 0;
        return (n * sumXY - sumX * sumY) / denom;
    }

    private String determineRiskByDays(int days) {
        if (days <= 3) return "critical";
        if (days <= 7) return "warning";
        if (days <= 14) return "attention";
        return "low";
    }

    private String determinePriorityByDays(int days) {
        if (days <= 3) return "urgent";
        if (days <= 7) return "high";
        if (days <= 14) return "medium";
        return "low";
    }

    private String generateSuggestedActions(Equipment eq, PredictionResult pred) {
        StringBuilder sb = new StringBuilder();
        String type = eq.getType() == null ? "" : eq.getType();
        sb.append("1. 全面设备状态检查，重点关注温度、振动等关键指标\n");
        switch (type) {
            case "pump":
                sb.append("2. 检查泵体密封、轴承润滑、叶轮磨损情况\n");
                sb.append("3. 检查进出口管道及阀门有无泄漏\n");
                sb.append("4. 更换润滑油/密封件\n");
                break;
            case "motor":
                sb.append("2. 检查电机绕组绝缘、轴承、冷却风扇\n");
                sb.append("3. 清洁电机散热片，检查接线端子\n");
                sb.append("4. 轴承加脂或更换\n");
                break;
            case "conveyor":
                sb.append("2. 检查输送带张力、磨损、接头\n");
                sb.append("3. 检查滚筒、托辊转动灵活性\n");
                sb.append("4. 检查减速器润滑油\n");
                break;
            case "robot":
                sb.append("2. 检查各关节轴减速器与伺服电机\n");
                sb.append("3. 检查线缆、气管有无老化破损\n");
                sb.append("4. 各关节加脂，零点校准\n");
                break;
            default:
                sb.append("2. 检查关键传动部件与润滑系统\n");
                sb.append("3. 检查电气控制与安全保护装置\n");
                sb.append("4. 按设备手册完成规定保养项目\n");
        }
        sb.append("5. 完成保养后重新采集运行数据验证指标改善\n");
        return sb.toString();
    }

    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }
    private double round4(double v) { return Math.round(v * 10000.0) / 10000.0; }
}
