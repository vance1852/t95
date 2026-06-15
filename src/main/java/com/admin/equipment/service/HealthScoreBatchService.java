package com.admin.equipment.service;

import com.admin.equipment.model.Equipment;
import com.admin.equipment.model.HealthScoreSnapshot;
import com.admin.equipment.repo.EquipmentRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class HealthScoreBatchService {

    private final EquipmentRepository equipmentRepo;
    private final HealthScoreEngine healthScoreEngine;
    private final PredictiveMaintenanceService predictiveService;
    private final AnomalyDetectionService anomalyService;

    private final ExecutorService executor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2));

    public HealthScoreBatchService(EquipmentRepository equipmentRepo,
                                   HealthScoreEngine healthScoreEngine,
                                   PredictiveMaintenanceService predictiveService,
                                   AnomalyDetectionService anomalyService) {
        this.equipmentRepo = equipmentRepo;
        this.healthScoreEngine = healthScoreEngine;
        this.predictiveService = predictiveService;
        this.anomalyService = anomalyService;
    }

    @Transactional
    public Map<String, Object> refreshAllHealthScores() {
        List<Equipment> all = equipmentRepo.findAll();
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        List<Long> failedIds = new ArrayList<>();

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (Equipment eq : all) {
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    healthScoreEngine.calculateAndSaveSnapshot(eq.getId());
                    success.incrementAndGet();
                } catch (Exception e) {
                    failed.incrementAndGet();
                    synchronized (failedIds) {
                        failedIds.add(eq.getId());
                    }
                }
            }, executor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return Map.of(
                "total", all.size(),
                "success", success.get(),
                "failed", failed.get(),
                "failedIds", failedIds,
                "finishedAt", LocalDateTime.now()
        );
    }

    @Transactional
    public Map<String, Object> runFullPipeline() {
        Map<String, Object> scoreResult = refreshAllHealthScores();

        int predCreated = 0;
        int alertsCreated = 0;

        for (Equipment eq : equipmentRepo.findAll()) {
            try {
                anomalyService.detectMetricAnomalies(eq.getId());
                anomalyService.detectHealthScoreDrop(eq.getId());
                alertsCreated++;
            } catch (Exception ignored) {}

            try {
                predictiveService.generatePredictiveOrder(eq.getId());
                predCreated++;
            } catch (Exception ignored) {}
        }

        return Map.of(
                "healthScoreRefresh", scoreResult,
                "anomalyDetectionRan", alertsCreated,
                "predictionGenerated", predCreated,
                "finishedAt", LocalDateTime.now()
        );
    }

    @Scheduled(cron = "0 0 */6 * * *")
    public void scheduledHealthScoreRefresh() {
        System.out.println("[定时任务] 开始刷新全部设备健康分: " + LocalDateTime.now());
        try {
            Map<String, Object> r = refreshAllHealthScores();
            System.out.println("[定时任务] 健康分刷新完成: " + r);
        } catch (Exception e) {
            System.err.println("[定时任务] 健康分刷新失败: " + e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void scheduledDailyPipeline() {
        System.out.println("[定时任务] 每日全量健康管道: " + LocalDateTime.now());
        try {
            Map<String, Object> r = runFullPipeline();
            System.out.println("[定时任务] 每日管道完成: " + r);
        } catch (Exception e) {
            System.err.println("[定时任务] 每日管道失败: " + e.getMessage());
        }
    }
}
