package com.admin.equipment.repo;

import com.admin.equipment.model.HealthScoreConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HealthScoreConfigRepository extends JpaRepository<HealthScoreConfig, Long> {
    Optional<HealthScoreConfig> findByConfigKey(String configKey);
}
