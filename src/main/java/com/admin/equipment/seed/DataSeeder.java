package com.admin.equipment.seed;

import com.admin.equipment.model.*;
import com.admin.equipment.repo.*;
import com.admin.equipment.security.PasswordUtil;
import com.admin.equipment.service.HealthScoreEngine;
import com.admin.equipment.service.PredictiveMaintenanceService;
import com.admin.equipment.service.RuntimeDataService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class DataSeeder implements CommandLineRunner {

    private final AppUserRepository userRepo;
    private final EquipmentRepository equipmentRepo;
    private final WorkOrderRepository workOrderRepo;
    private final InspectionRecordRepository inspectionRepo;
    private final HealthScoreConfigRepository configRepo;
    private final HealthScoreSnapshotRepository snapshotRepo;
    private final PredictiveMaintenanceOrderRepository predictionRepo;
    private final AnomalyAlertRepository alertRepo;
    private final PredictionFeedbackRepository feedbackRepo;
    private final RuntimeDataService runtimeDataService;
    private final HealthScoreEngine healthScoreEngine;
    private final PredictiveMaintenanceService predictiveService;

    @Value("${app.admin-username}")
    private String adminUsername;

    @Value("${app.admin-password}")
    private String adminPassword;

    private final Random random = new Random(42);

    public DataSeeder(AppUserRepository userRepo, EquipmentRepository equipmentRepo,
                      WorkOrderRepository workOrderRepo, InspectionRecordRepository inspectionRepo,
                      HealthScoreConfigRepository configRepo, HealthScoreSnapshotRepository snapshotRepo,
                      PredictiveMaintenanceOrderRepository predictionRepo, AnomalyAlertRepository alertRepo,
                      PredictionFeedbackRepository feedbackRepo, RuntimeDataService runtimeDataService,
                      HealthScoreEngine healthScoreEngine, PredictiveMaintenanceService predictiveService) {
        this.userRepo = userRepo;
        this.equipmentRepo = equipmentRepo;
        this.workOrderRepo = workOrderRepo;
        this.inspectionRepo = inspectionRepo;
        this.configRepo = configRepo;
        this.snapshotRepo = snapshotRepo;
        this.predictionRepo = predictionRepo;
        this.alertRepo = alertRepo;
        this.feedbackRepo = feedbackRepo;
        this.runtimeDataService = runtimeDataService;
        this.healthScoreEngine = healthScoreEngine;
        this.predictiveService = predictiveService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedAdmin();
        if (equipmentRepo.count() > 4) {
            System.out.println("种子数据已存在，跳过初始化");
            return;
        }
        seedDefaultConfig();
        seedEquipments();
        seedRuntimeData();
        seedWorkOrders();
        seedInspectionRecords();
        seedHealthScoreSnapshots();
        seedPredictionsAndAlerts();
        seedPredictionFeedbacks();
        System.out.println("预测性维护系统种子数据初始化完成");
    }

    private void seedAdmin() {
        if (!userRepo.existsByUsername(adminUsername)) {
            AppUser admin = new AppUser();
            admin.setUsername(adminUsername);
            admin.setPasswordHash(PasswordUtil.hash(adminPassword));
            admin.setDisplayName("平台管理员");
            userRepo.save(admin);
            System.out.println("已创建管理员账号");
        }
    }

    private void seedDefaultConfig() {
        if (!configRepo.findByConfigKey("default").isPresent()) {
            HealthScoreConfig cfg = new HealthScoreConfig();
            cfg.setConfigKey("default");
            configRepo.save(cfg);
        }
    }

    private void seedEquipments() {
        if (equipmentRepo.count() >= 10) return;

        List<Equipment> extras = List.of(
                newEquip("EQ-1005", "三号电机组", "生产车间B区", "motor", "normal"),
                newEquip("EQ-1006", "机械臂R-01", "装配线A", "robot", "warning"),
                newEquip("EQ-1007", "高压油泵站", "动力站", "pump", "normal"),
                newEquip("EQ-1008", "包装传送带B", "包装车间", "conveyor", "normal"),
                newEquip("EQ-1009", "机械臂R-02", "装配线B", "robot", "normal"),
                newEquip("EQ-1010", "冷却风机组", "动力站", "motor", "warning")
        );
        equipmentRepo.saveAll(extras);
    }

    private void seedRuntimeData() {
        int total = runtimeDataService.injectMockDataForAll(60, 12);
        System.out.println("已注入运行数据: " + total + " 条");
    }

    private void seedWorkOrders() {
        List<Equipment> equips = equipmentRepo.findAll();
        List<WorkOrder> orders = new ArrayList<>();

        String[] repairDescs = {
                "轴承异响更换", "密封圈泄漏更换", "电机绕组绝缘下降", "控制板故障更换",
                "液压油污染更换", "联轴器磨损更换", "传感器失灵校准"
        };
        String[] inspectors = {"王工", "李工", "张工", "赵工", "刘工"};

        for (Equipment eq : equips) {
            for (int i = 0; i < 2 + random.nextInt(3); i++) {
                int daysAgo = 5 + random.nextInt(150);
                WorkOrder w = new WorkOrder();
                w.setEquipmentId(eq.getId());
                int type = random.nextInt(3);
                if (type == 0) {
                    w.setTitle(eq.getName() + " - " + repairDescs[random.nextInt(repairDescs.length)]);
                    w.setType("repair");
                    w.setPriority(random.nextBoolean() ? "high" : "urgent");
                    w.setStatus(random.nextDouble() < 0.8 ? "done" : "in_progress");
                } else if (type == 1) {
                    w.setTitle(eq.getName() + " 定期保养");
                    w.setType("maintenance");
                    w.setPriority("medium");
                    w.setStatus("done");
                    w.setClosedAt(LocalDateTime.now().minusDays(daysAgo));
                } else {
                    w.setTitle(eq.getName() + " 日常巡检");
                    w.setType("inspection");
                    w.setPriority("low");
                    w.setStatus(random.nextDouble() < 0.9 ? "done" : "open");
                }
                w.setDescription("自动生成的种子工单数据");
                w.setAssignee(inspectors[random.nextInt(inspectors.length)]);
                w.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
                if ("done".equals(w.getStatus())) {
                    w.setClosedAt(LocalDateTime.now().minusDays(Math.max(1, daysAgo - random.nextInt(5))));
                }
                orders.add(w);
            }
        }
        workOrderRepo.saveAll(orders);
        System.out.println("已注入历史工单: " + orders.size() + " 条");
    }

    private void seedInspectionRecords() {
        List<Equipment> equips = equipmentRepo.findAll();
        List<InspectionRecord> records = new ArrayList<>();
        String[] inspectors = {"王工", "李工", "张工", "赵工", "刘工"};
        String[] anomalyItems = {
                "[\"温度偏高\", \"振动略大\"]",
                "[\"密封件有轻微渗油\"]",
                "[\"散热片积尘严重\"]",
                "[\"接线端子有氧化迹象\"]",
                "[\"润滑脂已干涸\"]",
                "[\"声音异常\"]"
        };

        for (Equipment eq : equips) {
            for (int i = 0; i < 3 + random.nextInt(5); i++) {
                int daysAgo = random.nextInt(90);
                boolean hasAnomaly = random.nextDouble() < 0.35;
                InspectionRecord r = new InspectionRecord();
                r.setEquipmentId(eq.getId());
                r.setTitle(eq.getName() + " 巡检记录");
                r.setHasAnomaly(hasAnomaly);
                if (hasAnomaly) {
                    int levelRoll = random.nextInt(3);
                    r.setAnomalyLevel(levelRoll == 0 ? "high" : (levelRoll == 1 ? "medium" : "low"));
                    r.setResult("abnormal");
                    r.setAnomalyItems(anomalyItems[random.nextInt(anomalyItems.length)]);
                    r.setDescription("巡检发现异常项，已登记");
                } else {
                    r.setAnomalyLevel("none");
                    r.setResult("normal");
                    r.setDescription("设备运行正常，无异常");
                }
                r.setInspector(inspectors[random.nextInt(inspectors.length)]);
                r.setInspectedAt(LocalDateTime.now().minusDays(daysAgo).minusHours(random.nextInt(12)));
                records.add(r);
            }
        }
        inspectionRepo.saveAll(records);
        System.out.println("已注入巡检记录: " + records.size() + " 条");
    }

    private void seedHealthScoreSnapshots() {
        List<Equipment> equips = equipmentRepo.findAll();
        int count = 0;
        for (Equipment eq : equips) {
            double baseTrend = switch (eq.getStatus()) {
                case "fault" -> 1.8;
                case "warning" -> 1.2;
                case "maintenance" -> 0.5;
                default -> 0.6;
            };
            HealthScoreEngine.ScoreResult result0 = healthScoreEngine.calculateHealthScore(eq.getId());
            for (int d = 30; d >= 0; d -= 1) {
                try {
                    HealthScoreSnapshot snap = new HealthScoreSnapshot();
                    snap.setEquipmentId(eq.getId());
                    snap.setSnapshotTime(LocalDateTime.now().minusDays(d).toLocalDate().atStartOfDay().plusHours(8));
                    double jitter = (random.nextDouble() - 0.5) * 4;
                    double scoreWithTrend = result0.totalScore + jitter + (d - 15) * baseTrend;
                    snap.setTotalScore(Math.max(15, Math.min(97, scoreWithTrend)));
                    snap.setMetricScore(Math.max(0, Math.min(100, result0.metricScore + (random.nextDouble() - 0.5) * 5)));
                    snap.setFaultScore(Math.max(0, Math.min(100, result0.faultScore)));
                    snap.setMaintenanceScore(Math.max(0, Math.min(100, result0.maintenanceScore)));
                    snap.setInspectionScore(Math.max(0, Math.min(100, result0.inspectionScore)));
                    snap.setMetricWeight(0.35);
                    snap.setFaultWeight(0.30);
                    snap.setMaintenanceWeight(0.20);
                    snap.setInspectionWeight(0.15);
                    if (snap.getTotalScore() >= 80) snap.setRiskLevel("good");
                    else if (snap.getTotalScore() >= 60) snap.setRiskLevel("warning");
                    else if (snap.getTotalScore() >= 40) snap.setRiskLevel("critical");
                    else snap.setRiskLevel("danger");
                    snap.setFactors("{}");
                    snapshotRepo.save(snap);
                    count++;
                } catch (Exception ignored) {}
            }
        }
        System.out.println("已生成健康分历史快照: " + count + " 条");
    }

    private void seedPredictionsAndAlerts() {
        List<Equipment> equips = equipmentRepo.findAll();
        int predCount = 0, alertCount = 0;

        for (Equipment eq : equips) {
            try {
                PredictiveMaintenanceOrder pred = predictiveService.generatePredictiveOrder(eq.getId());
                if (pred != null) predCount++;
            } catch (Exception ignored) {}

            if ("warning".equals(eq.getStatus()) || "fault".equals(eq.getStatus())) {
                AnomalyAlert a = new AnomalyAlert();
                a.setEquipmentId(eq.getId());
                a.setAlertType(random.nextBoolean() ? "metric" : "health_score");
                if ("metric".equals(a.getAlertType())) {
                    a.setMetricName("temperature_c");
                    a.setMetricValue(78.0 + random.nextDouble() * 15);
                    a.setThresholdValue(85.0);
                    a.setTitle("温度接近告警阈值");
                    a.setDescription("设备温度持续偏高，建议检查冷却系统");
                } else {
                    a.setHealthScoreDrop(10.0 + random.nextDouble() * 10);
                    a.setTitle("健康分近期下降明显");
                    a.setDescription("连续观察健康分呈下降趋势，建议安排检查");
                }
                a.setSeverity("warning".equals(eq.getStatus()) ? "warning" : "critical");
                a.setStatus("active");
                a.setDetectedAt(LocalDateTime.now().minusHours(random.nextInt(48)));
                alertRepo.save(a);
                alertCount++;
            }
        }
        System.out.println("已生成预测工单: " + predCount + " 条, 异常预警: " + alertCount + " 条");
    }

    private void seedPredictionFeedbacks() {
        List<PredictiveMaintenanceOrder> preds = predictionRepo.findAll();
        int count = 0;
        for (PredictiveMaintenanceOrder p : preds) {
            if (random.nextDouble() < 0.4) {
                PredictionFeedback fb = new PredictionFeedback();
                fb.setEquipmentId(p.getEquipmentId());
                fb.setPredictionId(p.getId());
                fb.setPredictedDate(p.getPredictedFaultDate());
                boolean occurred = random.nextDouble() < 0.7;
                fb.setFaultOccurred(occurred);
                if (occurred && p.getPredictedFaultDate() != null) {
                    int deviate = random.nextInt(10) - 3;
                    fb.setActualDate(p.getPredictedFaultDate().plusDays(deviate));
                    fb.setDaysDeviation(Math.abs(deviate));
                    fb.setAccuracyScore(Math.max(0, 100 - Math.abs(deviate) * 5.0));
                    fb.setFeedbackType("fault");
                } else {
                    fb.setAccuracyScore(occurred ? 70.0 : 0.0);
                }
                fb.setRemark("种子自动生成的预测回看数据");
                feedbackRepo.save(fb);
                p.setActualFaultOccurred(occurred);
                p.setPredictionAccuracy(fb.getAccuracyScore());
                p.setStatus("reviewed");
                predictionRepo.save(p);
                count++;
            }
        }
        System.out.println("已生成预测回看反馈: " + count + " 条");
    }

    private Equipment newEquip(String code, String name, String location, String type, String status) {
        Equipment e = new Equipment();
        e.setCode(code);
        e.setName(name);
        e.setLocation(location);
        e.setType(type);
        e.setStatus(status);
        return e;
    }
}
