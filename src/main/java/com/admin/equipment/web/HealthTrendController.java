package com.admin.equipment.web;

import com.admin.equipment.repo.EquipmentRepository;
import com.admin.equipment.service.HealthTrendService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/health-trends")
public class HealthTrendController {

    private final HealthTrendService trendService;
    private final EquipmentRepository equipmentRepo;

    public HealthTrendController(HealthTrendService trendService, EquipmentRepository equipmentRepo) {
        this.trendService = trendService;
        this.equipmentRepo = equipmentRepo;
    }

    @GetMapping("/equipment/{equipmentId}")
    public ResponseEntity<?> getTrend(@PathVariable Long equipmentId,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        if (!equipmentRepo.existsById(equipmentId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "设备不存在"));
        }
        if (from == null) from = LocalDateTime.now().minusDays(30);
        if (to == null) to = LocalDateTime.now();
        return ResponseEntity.ok(trendService.getTrendData(equipmentId, from, to));
    }

    @GetMapping("/distribution")
    public ResponseEntity<?> getDistribution() {
        return ResponseEntity.ok(trendService.getHealthDistribution());
    }

    @GetMapping("/ranking")
    public ResponseEntity<?> getRanking(@RequestParam(defaultValue = "20") int limit,
                                        @RequestParam(defaultValue = "true") boolean worstFirst) {
        return ResponseEntity.ok(trendService.getHealthRanking(limit, worstFirst));
    }

    @GetMapping("/factory-overview")
    public ResponseEntity<?> getFactoryOverview() {
        return ResponseEntity.ok(trendService.getFactoryHealthOverview());
    }
}
