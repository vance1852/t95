package com.admin.equipment.repo;

import com.admin.equipment.model.PredictionFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PredictionFeedbackRepository extends JpaRepository<PredictionFeedback, Long> {

    List<PredictionFeedback> findByEquipmentIdOrderByCreatedAtDesc(Long equipmentId);

    @Query("SELECT AVG(f.accuracyScore) FROM PredictionFeedback f WHERE f.createdAt >= :from")
    Double findAverageAccuracySince(@Param("from") LocalDateTime from);

    @Query("SELECT COUNT(f) FROM PredictionFeedback f WHERE f.faultOccurred = true AND f.createdAt >= :from")
    long countCorrectPredictions(@Param("from") LocalDateTime from);

    @Query("SELECT COUNT(f) FROM PredictionFeedback f WHERE f.createdAt >= :from")
    long countTotalPredictions(@Param("from") LocalDateTime from);
}
