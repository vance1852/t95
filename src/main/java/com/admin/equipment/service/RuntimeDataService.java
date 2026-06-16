package com.admin.equipment.service;

import com.admin.equipment.model.Equipment;
import com.admin.equipment.model.EquipmentRuntimeData;
import com.admin.equipment.repo.EquipmentRepository;
import com.admin.equipment.repo.EquipmentRuntimeDataRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class RuntimeDataService {

    private final EquipmentRuntimeDataRepository runtimeRepo;
    private final EquipmentRepository equipmentRepo;
    private final Random random = new Random();

    public RuntimeDataService(EquipmentRuntimeDataRepository runtimeRepo,
                              EquipmentRepository equipmentRepo) {
        this.runtimeRepo = runtimeRepo;
        this.equipmentRepo = equipmentRepo;
    }

    public EquipmentRuntimeData reportRuntimeData(Long equipmentId, EquipmentRuntimeData data) {
        if (!equipmentRepo.existsById(equipmentId)) {
            throw new IllegalArgumentException("设备不存在");
        }
        data.setEquipmentId(equipmentId);
        if (data.getRecordedAt() == null) {
            data.setRecordedAt(LocalDateTime.now());
        }
        return runtimeRepo.save(data);
    }

    public List<EquipmentRuntimeData> batchReport(List<EquipmentRuntimeData> dataList) {
        List<EquipmentRuntimeData> saved = new ArrayList<>();
        for (EquipmentRuntimeData d : dataList) {
            if (d.getRecordedAt() == null) d.setRecordedAt(LocalDateTime.now());
            saved.add(runtimeRepo.save(d));
        }
        return saved;
    }

    public List<EquipmentRuntimeData> getRecentData(Long equipmentId, int limit) {
        return runtimeRepo.findLatestByEquipmentId(equipmentId, limit);
    }

    public List<EquipmentRuntimeData> getDataBetween(Long equipmentId, LocalDateTime from, LocalDateTime to) {
        return runtimeRepo.findByEquipmentIdAndRecordedAtBetween(equipmentId, from, to);
    }

    public int injectMockDataForEquipment(Long equipmentId, int days, int samplesPerDay) {
        Equipment eq = equipmentRepo.findById(equipmentId).orElse(null);
        if (eq == null) return 0;

        List<EquipmentRuntimeData> batch = new ArrayList<>();
        String type = eq.getType() == null ? "" : eq.getType();

        double baseTemp = switch (type) {
            case "pump" -> 55.0;
            case "motor" -> 60.0;
            case "conveyor" -> 45.0;
            case "robot" -> 50.0;
            default -> 52.0;
        };
        double baseVib = switch (type) {
            case "pump" -> 2.8;
            case "motor" -> 3.2;
            case "conveyor" -> 1.8;
            case "robot" -> 2.2;
            default -> 2.5;
        };
        double baseLoad = switch (type) {
            case "pump" -> 65.0;
            case "motor" -> 70.0;
            case "conveyor" -> 50.0;
            case "robot" -> 55.0;
            default -> 60.0;
        };

        double healthDegrade = switch (eq.getStatus()) {
            case "fault" -> 2.5;
            case "warning" -> 1.5;
            case "maintenance" -> 0.8;
            default -> 1.0;
        };

        double cumulativeHours = 1000 + random.nextDouble() * 6000;
        LocalDateTime now = LocalDateTime.now();
        int injected = 0;

        for (int d = days - 1; d >= 0; d--) {
            LocalDateTime dayStart = LocalDateTime.now().minusDays(d).toLocalDate().atStartOfDay();
            for (int s = 0; s < samplesPerDay; s++) {
                LocalDateTime recordedAt = dayStart.plusHours((long) (24.0 * s / samplesPerDay))
                        .plusMinutes(random.nextInt(59));
                if (recordedAt.isAfter(now)) {
                    continue;
                }

                EquipmentRuntimeData rd = new EquipmentRuntimeData();
                rd.setEquipmentId(equipmentId);
                rd.setRecordedAt(recordedAt);

                double degradeFactor = 1 + (days - 1 - d) * 0.008 * healthDegrade;
                double tempSpike = (random.nextDouble() < 0.03) ? random.nextDouble() * 20 : 0;
                double vibSpike = (random.nextDouble() < 0.03) ? random.nextDouble() * 4 : 0;

                rd.setTemperatureC(round(baseTemp * degradeFactor + random.nextGaussian() * 3 + tempSpike));
                rd.setVibrationMmS(round(baseVib * degradeFactor + Math.abs(random.nextGaussian() * 0.5) + vibSpike));
                rd.setLoadPercent(round(Math.max(5, Math.min(99, baseLoad + random.nextGaussian() * 10))));
                rd.setRunningHours(round(cumulativeHours));
                cumulativeHours += (8.0 / samplesPerDay) * (0.8 + random.nextDouble() * 0.4);

                if (type.equals("pump") || type.equals("motor")) {
                    rd.setPressureMPa(round(0.5 + random.nextGaussian() * 0.1));
                    rd.setCurrentAmp(round(15 + random.nextGaussian() * 3));
                }
                if (type.equals("motor") || type.equals("robot")) {
                    rd.setRpm(round(1450 + random.nextGaussian() * 50));
                }

                batch.add(rd);
                if (batch.size() >= 500) {
                    runtimeRepo.saveAll(batch);
                    batch.clear();
                }
            }
        }
        if (!batch.isEmpty()) runtimeRepo.saveAll(batch);
        return days * samplesPerDay;
    }

    public int injectMockDataForAll(int days, int samplesPerDay) {
        int total = 0;
        for (Equipment eq : equipmentRepo.findAll()) {
            total += injectMockDataForEquipment(eq.getId(), days, samplesPerDay);
        }
        return total;
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
