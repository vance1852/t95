package com.admin.equipment.web;

import com.admin.equipment.model.InspectionRecord;
import com.admin.equipment.model.PredictionFeedback;
import com.admin.equipment.repo.EquipmentRepository;
import com.admin.equipment.repo.InspectionRecordRepository;
import com.admin.equipment.service.EquipmentHealthProfileService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/equipment-profile")
public class EquipmentProfileController {

    private final EquipmentHealthProfileService profileService;
    private final EquipmentRepository equipmentRepo;
    private final InspectionRecordRepository inspectionRepo;

    public EquipmentProfileController(EquipmentHealthProfileService profileService,
                                      EquipmentRepository equipmentRepo,
                                      InspectionRecordRepository inspectionRepo) {
        this.profileService = profileService;
        this.equipmentRepo = equipmentRepo;
        this.inspectionRepo = inspectionRepo;
    }

    @GetMapping("/{equipmentId}")
    public ResponseEntity<?> getProfile(@PathVariable Long equipmentId) {
        if (!equipmentRepo.existsById(equipmentId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "设备不存在"));
        }
        return ResponseEntity.ok(profileService.getHealthProfile(equipmentId));
    }

    @PostMapping("/inspection")
    public ResponseEntity<?> createInspection(@RequestBody InspectionRequest req) {
        if (req.equipmentId() == null || !equipmentRepo.existsById(req.equipmentId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "设备不存在"));
        }
        InspectionRecord r = new InspectionRecord();
        r.setEquipmentId(req.equipmentId());
        r.setWorkOrderId(req.workOrderId());
        r.setTitle(req.title() == null ? "巡检记录" : req.title());
        r.setResult(req.result() == null ? "normal" : req.result());
        r.setHasAnomaly(Boolean.TRUE.equals(req.hasAnomaly()));
        r.setAnomalyLevel(req.anomalyLevel() == null ? (Boolean.TRUE.equals(req.hasAnomaly()) ? "medium" : "none") : req.anomalyLevel());
        r.setAnomalyItems(req.anomalyItems());
        r.setDescription(req.description() == null ? "" : req.description());
        r.setInspector(req.inspector() == null ? "" : req.inspector());
        r.setInspectedAt(req.inspectedAt() == null ? LocalDateTime.now() : req.inspectedAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(inspectionRepo.save(r));
    }

    @GetMapping("/prediction-accuracy")
    public ResponseEntity<?> getPredictionAccuracy(@RequestParam(defaultValue = "90") int days) {
        return ResponseEntity.ok(profileService.getPredictionAccuracyOverview(days));
    }

    @PostMapping("/prediction-feedback")
    public ResponseEntity<?> recordFeedback(@RequestBody FeedbackRequest req) {
        PredictionFeedback fb = profileService.recordPredictionFeedback(
                req.predictionId(),
                Boolean.TRUE.equals(req.faultOccurred()),
                req.actualDate(),
                req.remark()
        );
        if (fb == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "预测工单不存在"));
        }
        return ResponseEntity.ok(fb);
    }

    public record InspectionRequest(Long equipmentId, Long workOrderId, String title, String result,
                                    Boolean hasAnomaly, String anomalyLevel, String anomalyItems,
                                    String description, String inspector,
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inspectedAt) {}

    public record FeedbackRequest(Long predictionId, Boolean faultOccurred,
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime actualDate,
                                   String remark) {}
}
