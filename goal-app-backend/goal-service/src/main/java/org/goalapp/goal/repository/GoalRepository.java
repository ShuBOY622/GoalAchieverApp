package org.goalapp.goal.repository;

import org.goalapp.goal.entities.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {

    // ✅ Same method names - no service changes needed
    List<Goal> findByCreatedBy(Long userId);
    List<Goal> findByType(Goal.GoalType type);
    List<Goal> findByDeadlineBefore(LocalDateTime deadline);

    // ✅ Only change: Added ORDER BY g.createdAt DESC for newest first
    @Query("SELECT g FROM Goal g JOIN GoalAssignment ga ON g.id = ga.goalId WHERE ga.userId = ?1 ORDER BY g.createdAt DESC")
    List<Goal> findGoalsByUserId(Long userId);

    // ✅ Only change: Added ORDER BY g.createdAt DESC for newest search results
    @Query("SELECT g FROM Goal g WHERE g.title LIKE %?1% OR g.description LIKE %?1% ORDER BY g.createdAt DESC")
    List<Goal> searchGoals(String query);
}
