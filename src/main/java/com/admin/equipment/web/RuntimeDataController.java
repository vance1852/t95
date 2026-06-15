package com.admin.equipment.web;

import com.admin.equipment.model.EquipmentRuntimeData;
import com.admin.equipment.repo.EquipmentRepository;
import com.admin.equipment.service.RuntimeDataService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/runtime-data")
public class RuntimeDataController {

    private final RuntimeDataService runtimeService;
    private final EquipmentRepository equipmentRepo;

    public RuntimeDataController(RuntimeDataService runtimeService, EquipmentRepository equipmentRepo) {
        this.runtimeService = runtimeService;
        this.equipmentRepo = equipmentRepo;
    }

    public record RuntimeDataRequest(Double runningHours, Double loadPercent, Double temperatureC,
                                      Double vibrationMmS, Double pressureMPa, Double currentAmp,
                                      Double rpm, String remark) {}

    @PostMapping("/equipment/{equipmentId}")
    public ResponseEntity<?> report(@PathVariable Long equipmentId, @RequestBody RuntimeDataRequest req) {
        if (!equipmentRepo.existsById(equipmentId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "设备不存在"));
        }
        EquipmentRuntimeData d = new EquipmentRuntimeData();
        d.setRunningHours(req.runningHours());
        d.setLoadPercent(req.loadPercent());
        d.setTemperatureC(req.temperatureC());
        d.setVibrationMmS(req.vibrationMmS());
        d.setPressureMPa(req.pressureMPa());
        d.setCurrentAmp(req.currentAmp());
        d.setRpm(req.rpm());
        d.setRemark(req.remark() == null ? "" : req.remark());
        return ResponseEntity.status(HttpStatus.CREATED).body(runtimeService.reportRuntimeData(equipmentId, d));
    }

    @PostMapping("/batch")
    public ResponseEntity<?> batchReport(@RequestBody List<EquipmentRuntimeData> data) {
        return ResponseEntity.ok(Map.of("saved", runtimeService.batchReport(data).size()));
    }

    @GetMapping("/equipment/{equipmentId}")
    public ResponseEntity<?> getRecent(@PathVariable Long equipmentId,
                                       @RequestParam(defaultValue = "50") int limit) {
        if (!equipmentRepo.existsById(equipmentId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "设备不存在"));
        }
        return ResponseEntity.ok(runtimeService.getRecentData(equipmentId, limit));
    }

    @GetMapping("/equipment/{equipmentId}/range")
    public ResponseEntity<?> getRange(@PathVariable Long equipmentId,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        if (!equipmentRepo.existsById(equipmentId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "设备不存在"));
        }
        return ResponseEntity.ok(runtimeService.getDataBetween(equipmentId, from, to));
    }

    @PostMapping("/inject-mock")
    public ResponseEntity<?> injectMock(@RequestParam(required = false) Long equipmentId,
                                        @RequestParam(defaultValue = "30") int days,
                                        @RequestParam(defaultValue = "24") int samplesPerDay) {
        int count;
        if (equipmentId != null) {
            if (!equipmentRepo.existsById(equipmentId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "设备不存在"));
            }
            count = runtimeService.injectMockDataForEquipment(equipmentId, days, samplesPerDay);
        } else {
            count = runtimeService.injectMockDataForAll(days, samplesPerDay);
        }
        return ResponseEntity.ok(Map.of("injectedRecords", count, "days", days, "samplesPerDay", samplesPerDay));
    }
}
