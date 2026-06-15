package com.admin.equipment.web;

import com.admin.equipment.model.HealthScoreConfig;
import com.admin.equipment.model.HealthScoreSnapshot;
import com.admin.equipment.repo.EquipmentRepository;
import com.admin.equipment.repo.HealthScoreConfigRepository;
import com.admin.equipment.repo.HealthScoreSnapshotRepository;
import com.admin.equipment.service.HealthScoreBatchService;
import com.admin.equipment.service.HealthScoreEngine;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/health-scores")
public class HealthScoreController {

    private final HealthScoreEngine healthScoreEngine;
    private final HealthScoreBatchService batchService;
    private final HealthScoreSnapshotRepository snapshotRepo;
    private final HealthScoreConfigRepository configRepo;
    private final EquipmentRepository equipmentRepo;

    public HealthScoreController(HealthScoreEngine healthScoreEngine,
                                 HealthScoreBatchService batchService,
                                 HealthScoreSnapshotRepository snapshotRepo,
                                 HealthScoreConfigRepository configRepo,
                                 EquipmentRepository equipmentRepo) {
        this.healthScoreEngine = healthScoreEngine;
        this.batchService = batchService;
        this.snapshotRepo = snapshotRepo;
        this.configRepo = configRepo;
        this.equipmentRepo = equipmentRepo;
    }

    @GetMapping("/equipment/{equipmentId}")
    public ResponseEntity<?> calculate(@PathVariable Long equipmentId) {
        if (!equipmentRepo.existsById(equipmentId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "设备不存在"));
        }
        return ResponseEntity.ok(healthScoreEngine.calculateHealthScore(equipmentId));
    }

    @PostMapping("/equipment/{equipmentId}/snapshot")
    public ResponseEntity<?> createSnapshot(@PathVariable Long equipmentId) {
        if (!equipmentRepo.existsById(equipmentId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "设备不存在"));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(healthScoreEngine.calculateAndSaveSnapshot(equipmentId));
    }

    @GetMapping("/equipment/{equipmentId}/latest")
    public ResponseEntity<?> getLatest(@PathVariable Long equipmentId) {
        if (!equipmentRepo.existsById(equipmentId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "设备不存在"));
        }
        return snapshotRepo.findLatestByEquipmentId(equipmentId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(Map.of("detail", "暂无健康分快照")));
    }

    @GetMapping("/equipment/{equipmentId}/history")
    public ResponseEntity<?> getHistory(@PathVariable Long equipmentId,
                                        @RequestParam(defaultValue = "30") int days) {
        if (!equipmentRepo.existsById(equipmentId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "设备不存在"));
        }
        LocalDateTime from = LocalDateTime.now().minusDays(days);
        List<HealthScoreSnapshot> list = snapshotRepo.findByEquipmentIdAndSnapshotTimeAfter(equipmentId, from);
        return ResponseEntity.ok(list);
    }

    @PostMapping("/refresh-all")
    public ResponseEntity<?> refreshAll() {
        return ResponseEntity.ok(batchService.refreshAllHealthScores());
    }

    @PostMapping("/full-pipeline")
    public ResponseEntity<?> fullPipeline() {
        return ResponseEntity.ok(batchService.runFullPipeline());
    }

    @GetMapping("/config")
    public ResponseEntity<?> getConfig() {
        return ResponseEntity.ok(configRepo.findByConfigKey("default").orElseGet(() -> {
            HealthScoreConfig c = new HealthScoreConfig();
            c.setConfigKey("default");
            return c;
        }));
    }

    @PutMapping("/config")
    public ResponseEntity<?> updateConfig(@RequestBody HealthScoreConfig req) {
        HealthScoreConfig cfg = configRepo.findByConfigKey("default").orElseGet(() -> {
            HealthScoreConfig c = new HealthScoreConfig();
            c.setConfigKey("default");
            return c;
        });
        if (req.getMetricWeight() != null) cfg.setMetricWeight(req.getMetricWeight());
        if (req.getFaultWeight() != null) cfg.setFaultWeight(req.getFaultWeight());
        if (req.getMaintenanceWeight() != null) cfg.setMaintenanceWeight(req.getMaintenanceWeight());
        if (req.getInspectionWeight() != null) cfg.setInspectionWeight(req.getInspectionWeight());
        if (req.getTemperatureNormalMax() != null) cfg.setTemperatureNormalMax(req.getTemperatureNormalMax());
        if (req.getTemperatureWarningMax() != null) cfg.setTemperatureWarningMax(req.getTemperatureWarningMax());
        if (req.getVibrationNormalMax() != null) cfg.setVibrationNormalMax(req.getVibrationNormalMax());
        if (req.getVibrationWarningMax() != null) cfg.setVibrationWarningMax(req.getVibrationWarningMax());
        if (req.getMaintenanceIntervalDays() != null) cfg.setMaintenanceIntervalDays(req.getMaintenanceIntervalDays());
        if (req.getMaintenanceWarningDays() != null) cfg.setMaintenanceWarningDays(req.getMaintenanceWarningDays());
        if (req.getFaultWindowDays() != null) cfg.setFaultWindowDays(req.getFaultWindowDays());
        if (req.getFaultNormalMax() != null) cfg.setFaultNormalMax(req.getFaultNormalMax());
        if (req.getRiskThresholdGood() != null) cfg.setRiskThresholdGood(req.getRiskThresholdGood());
        if (req.getRiskThresholdWarning() != null) cfg.setRiskThresholdWarning(req.getRiskThresholdWarning());
        if (req.getRiskThresholdCritical() != null) cfg.setRiskThresholdCritical(req.getRiskThresholdCritical());
        cfg.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(configRepo.save(cfg));
    }
}
