package com.admin.equipment.service;

import com.admin.equipment.model.Equipment;
import com.admin.equipment.model.HealthScoreSnapshot;
import com.admin.equipment.repo.EquipmentRepository;
import com.admin.equipment.repo.HealthScoreSnapshotRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HealthTrendService {

    private final HealthScoreSnapshotRepository snapshotRepo;
    private final EquipmentRepository equipmentRepo;

    public HealthTrendService(HealthScoreSnapshotRepository snapshotRepo,
                              EquipmentRepository equipmentRepo) {
        this.snapshotRepo = snapshotRepo;
        this.equipmentRepo = equipmentRepo;
    }

    public static class TrendPoint {
        public LocalDateTime time;
        public Double score;
        public TrendPoint(LocalDateTime t, Double s) { this.time = t; this.score = s; }
    }

    public static class TrendData {
        public Long equipmentId;
        public String equipmentCode;
        public String equipmentName;
        public List<TrendPoint> points;
        public Double latestScore;
        public Double scoreChange;
        public String riskLevel;
    }

    public static class DistributionBucket {
        public String label;
        public String range;
        public long count;
        public List<Equipment> equipments;
    }

    public static class RankingItem {
        public Long equipmentId;
        public String code;
        public String name;
        public String location;
        public String type;
        public Double healthScore;
        public String riskLevel;
        public Double scoreChange7d;
    }

    public TrendData getTrendData(Long equipmentId, LocalDateTime from, LocalDateTime to) {
        Equipment eq = equipmentRepo.findById(equipmentId).orElse(null);
        TrendData td = new TrendData();
        if (eq == null) return td;

        td.equipmentId = eq.getId();
        td.equipmentCode = eq.getCode();
        td.equipmentName = eq.getName();

        List<HealthScoreSnapshot> snapshots = snapshotRepo
                .findByEquipmentIdAndSnapshotTimeBetween(equipmentId, from, to);

        td.points = snapshots.stream()
                .map(s -> new TrendPoint(s.getSnapshotTime(), s.getTotalScore()))
                .collect(Collectors.toList());

        if (!snapshots.isEmpty()) {
            td.latestScore = snapshots.get(snapshots.size() - 1).getTotalScore();
            td.riskLevel = snapshots.get(snapshots.size() - 1).getRiskLevel();
            if (snapshots.size() >= 2) {
                td.scoreChange = round2(td.latestScore - snapshots.get(0).getTotalScore());
            }
        }

        return td;
    }

    public List<DistributionBucket> getHealthDistribution() {
        List<HealthScoreSnapshot> latest = snapshotRepo.findLatestForAllEquipments();
        Map<Long, Equipment> eqMap = equipmentRepo.findAll().stream()
                .collect(Collectors.toMap(Equipment::getId, e -> e));

        List<DistributionBucket> buckets = new ArrayList<>();
        String[][] ranges = {
                {"优秀", "90-100"},
                {"良好", "80-89"},
                {"一般", "60-79"},
                {"预警", "40-59"},
                {"危险", "0-39"}
        };
        int[][] thresholds = {{90, 101}, {80, 90}, {60, 80}, {40, 60}, {0, 40}};

        for (int i = 0; i < ranges.length; i++) {
            DistributionBucket b = new DistributionBucket();
            b.label = ranges[i][0];
            b.range = ranges[i][1];
            b.equipments = new ArrayList<>();
            int lo = thresholds[i][0], hi = thresholds[i][1];
            for (HealthScoreSnapshot s : latest) {
                if (s.getTotalScore() >= lo && s.getTotalScore() < hi) {
                    b.count++;
                    Equipment eq = eqMap.get(s.getEquipmentId());
                    if (eq != null) b.equipments.add(eq);
                }
            }
            buckets.add(b);
        }
        return buckets;
    }

    public List<RankingItem> getHealthRanking(int limit, boolean ascending) {
        List<HealthScoreSnapshot> latest = snapshotRepo.findLatestForAllEquipments();
        Map<Long, Equipment> eqMap = equipmentRepo.findAll().stream()
                .collect(Collectors.toMap(Equipment::getId, e -> e));

        List<RankingItem> items = new ArrayList<>();
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);

        for (HealthScoreSnapshot s : latest) {
            Equipment eq = eqMap.get(s.getEquipmentId());
            if (eq == null) continue;
            RankingItem r = new RankingItem();
            r.equipmentId = eq.getId();
            r.code = eq.getCode();
            r.name = eq.getName();
            r.location = eq.getLocation();
            r.type = eq.getType();
            r.healthScore = s.getTotalScore();
            r.riskLevel = s.getRiskLevel();

            List<HealthScoreSnapshot> old = snapshotRepo
                    .findByEquipmentIdAndSnapshotTimeAfter(eq.getId(), weekAgo);
            if (!old.isEmpty()) {
                r.scoreChange7d = round2(s.getTotalScore() - old.get(0).getTotalScore());
            } else {
                r.scoreChange7d = 0.0;
            }
            items.add(r);
        }

        items.sort((a, b) -> ascending
                ? Double.compare(a.healthScore, b.healthScore)
                : Double.compare(b.healthScore, a.healthScore));

        return items.stream().limit(limit).collect(Collectors.toList());
    }

    public Map<String, Object> getFactoryHealthOverview() {
        List<HealthScoreSnapshot> latest = snapshotRepo.findLatestForAllEquipments();
        Map<String, Object> result = new LinkedHashMap<>();

        long total = equipmentRepo.count();
        result.put("totalEquipment", total);
        result.put("scoredCount", latest.size());

        if (!latest.isEmpty()) {
            double avg = latest.stream().mapToDouble(HealthScoreSnapshot::getTotalScore).average().orElse(0);
            result.put("averageScore", round2(avg));
            result.put("minScore", latest.stream().mapToDouble(HealthScoreSnapshot::getTotalScore).min().orElse(0));
            result.put("maxScore", latest.stream().mapToDouble(HealthScoreSnapshot::getTotalScore).max().orElse(0));

            long good = latest.stream().filter(s -> s.getTotalScore() >= 80).count();
            long warn = latest.stream().filter(s -> s.getTotalScore() >= 60 && s.getTotalScore() < 80).count();
            long crit = latest.stream().filter(s -> s.getTotalScore() < 60).count();
            result.put("goodCount", good);
            result.put("warningCount", warn);
            result.put("criticalCount", crit);
            result.put("goodRate", round2(good * 100.0 / latest.size()));
        }

        return result;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
