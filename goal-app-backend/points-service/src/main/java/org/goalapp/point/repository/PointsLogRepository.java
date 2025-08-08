package org.goalapp.point.repository;

import org.goalapp.point.entities.PointsLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PointsLogRepository extends JpaRepository<PointsLog, Long> {
    List<PointsLog> findByUserId(Long userId);
    List<PointsLog> findByGoalId(Long goalId);
    List<PointsLog> findByUserIdAndLoggedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT SUM(pl.pointsChange) FROM PointsLog pl WHERE pl.userId = ?1")
    Integer getTotalPointsByUserId(Long userId);

    @Query("SELECT COUNT(pl) FROM PointsLog pl WHERE pl.userId = ?1 AND pl.pointsChange > 0")
    Long getPositivePointsCount(Long userId);

    @Query("SELECT COUNT(pl) FROM PointsLog pl WHERE pl.userId = ?1 AND pl.pointsChange < 0")
    Long getNegativePointsCount(Long userId);
}
