package com.admin.equipment.web;

import com.admin.equipment.model.PredictiveMaintenanceOrder;
import com.admin.equipment.model.WorkOrder;
import com.admin.equipment.repo.EquipmentRepository;
import com.admin.equipment.service.PredictiveMaintenanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/predictive-maintenance")
public class PredictiveMaintenanceController {

    private final PredictiveMaintenanceService predictiveService;
    private final EquipmentRepository equipmentRepo;

    public PredictiveMaintenanceController(PredictiveMaintenanceService predictiveService,
                                            EquipmentRepository equipmentRepo) {
        this.predictiveService = predictiveService;
        this.equipmentRepo = equipmentRepo;
    }

    @GetMapping("/predict/{equipmentId}")
    public ResponseEntity<?> predict(@PathVariable Long equipmentId) {
        if (!equipmentRepo.existsById(equipmentId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "设备不存在"));
        }
        return ResponseEntity.ok(predictiveService.predictRemainingHealth(equipmentId));
    }

    @PostMapping("/generate/{equipmentId}")
    public ResponseEntity<?> generateOrder(@PathVariable Long equipmentId) {
        if (!equipmentRepo.existsById(equipmentId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "设备不存在"));
        }
        PredictiveMaintenanceOrder order = predictiveService.generatePredictiveOrder(equipmentId);
        if (order == null) {
            return ResponseEntity.ok(Map.of("detail", "该设备暂无需生成预测工单（健康状态良好或数据不足）"));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @PostMapping("/{predictionId}/convert")
    public ResponseEntity<?> convertToWorkOrder(@PathVariable Long predictionId,
                                                @RequestBody(required = false) Map<String, String> body) {
        String assignee = body != null ? body.get("assignee") : null;
        WorkOrder wo = predictiveService.convertPredictionToWorkOrder(predictionId, assignee);
        if (wo == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "预测工单不存在"));
        }
        return ResponseEntity.ok(wo);
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPending(@RequestParam(required = false) Integer maxDays) {
        return ResponseEntity.ok(predictiveService.getPendingPredictions(maxDays));
    }

    public record ConvertRequest(String assignee) {}
}
