package com.admin.equipment.web;

import com.admin.equipment.model.AnomalyAlert;
import com.admin.equipment.repo.AnomalyAlertRepository;
import com.admin.equipment.repo.EquipmentRepository;
import com.admin.equipment.service.AnomalyDetectionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/anomaly-alerts")
public class AnomalyAlertController {

    private final AnomalyDetectionService anomalyService;
    private final AnomalyAlertRepository alertRepo;
    private final EquipmentRepository equipmentRepo;

    public AnomalyAlertController(AnomalyDetectionService anomalyService,
                                  AnomalyAlertRepository alertRepo,
                                  EquipmentRepository equipmentRepo) {
        this.anomalyService = anomalyService;
        this.alertRepo = alertRepo;
        this.equipmentRepo = equipmentRepo;
    }

    @PostMapping("/detect/{equipmentId}")
    public ResponseEntity<?> detect(@PathVariable Long equipmentId) {
        if (!equipmentRepo.existsById(equipmentId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "设备不存在"));
        }
        List<AnomalyAlert> all = new ArrayList<>();
        all.addAll(anomalyService.detectMetricAnomalies(equipmentId));
        all.addAll(anomalyService.detectHealthScoreDrop(equipmentId));
        return ResponseEntity.ok(Map.of("newAlerts", all.size(), "alerts", all));
    }

    @GetMapping("/active")
    public ResponseEntity<?> getActive() {
        return ResponseEntity.ok(anomalyService.getActiveAlerts());
    }

    @GetMapping("/recent")
    public ResponseEntity<?> getRecent(@RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(anomalyService.getRecentAlerts(days));
    }

    @GetMapping("/equipment/{equipmentId}")
    public ResponseEntity<?> getByEquipment(@PathVariable Long equipmentId) {
        if (!equipmentRepo.existsById(equipmentId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "设备不存在"));
        }
        return ResponseEntity.ok(alertRepo.findByEquipmentIdOrderByDetectedAtDesc(equipmentId));
    }

    @PostMapping("/{alertId}/acknowledge")
    public ResponseEntity<?> acknowledge(@PathVariable Long alertId,
                                         @RequestBody(required = false) Map<String, String> body) {
        String by = body != null ? body.get("by") : null;
        AnomalyAlert a = anomalyService.acknowledgeAlert(alertId, by);
        if (a == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "预警不存在"));
        }
        return ResponseEntity.ok(a);
    }
}
