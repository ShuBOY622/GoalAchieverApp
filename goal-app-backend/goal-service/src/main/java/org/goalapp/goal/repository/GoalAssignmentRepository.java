package org.goalapp.goal.repository;

import org.goalapp.goal.entities.GoalAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoalAssignmentRepository extends JpaRepository<GoalAssignment, Long> {
    List<GoalAssignment> findByUserId(Long userId);
    List<GoalAssignment> findByGoalId(Long goalId);
    List<GoalAssignment> findByStatus(GoalAssignment.Status status);
    Optional<GoalAssignment> findByGoalIdAndUserId(Long goalId, Long userId);

    @Query("SELECT ga FROM GoalAssignment ga WHERE ga.userId = ?1 AND ga.status = ?2")
    List<GoalAssignment> findByUserIdAndStatus(Long userId, GoalAssignment.Status status);

    @Query("SELECT COUNT(ga) FROM GoalAssignment ga WHERE ga.userId = ?1 AND ga.status = 'COMPLETED'")
    Long countCompletedGoalsByUser(Long userId);

    @Query("SELECT COUNT(ga) FROM GoalAssignment ga WHERE ga.userId = ?1 AND ga.status = 'MISSED'")
    Long countMissedGoalsByUser(Long userId);
}
