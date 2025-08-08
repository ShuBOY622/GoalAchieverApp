package org.goalapp.goal.controller;

import org.goalapp.goal.dto.GoalCreateDto;
import org.goalapp.goal.dto.GoalResponseDto;
import org.goalapp.goal.service.GoalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    @Autowired
    private GoalService goalService;





    @PostMapping
    public ResponseEntity<GoalResponseDto> createGoal(@Valid @RequestBody GoalCreateDto goalCreateDto) {
        try {
            GoalResponseDto goal = goalService.createGoalDto(goalCreateDto); // ✅ Fixed method call
            return ResponseEntity.ok(goal);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<GoalResponseDto> getGoalById(@PathVariable Long id) {
        Optional<GoalResponseDto> goal = goalService.getGoalById(id);
        return goal.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<GoalResponseDto>> getGoalsByUserId(@PathVariable Long userId) {
        List<GoalResponseDto> goals = goalService.getGoalsByUserId(userId);
        return ResponseEntity.ok(goals);
    }

    @GetMapping("/created-by/{creatorId}")
    public ResponseEntity<List<GoalResponseDto>> getGoalsByCreator(@PathVariable Long creatorId) {
        List<GoalResponseDto> goals = goalService.getGoalsByCreator(creatorId);
        return ResponseEntity.ok(goals);
    }

    @GetMapping("/search")
    public ResponseEntity<List<GoalResponseDto>> searchGoals(@RequestParam String query) {
        List<GoalResponseDto> goals = goalService.searchGoals(query);
        return ResponseEntity.ok(goals);
    }

    @PutMapping("/{goalId}/complete")
    public ResponseEntity<GoalResponseDto> completeGoal(
            @PathVariable Long goalId,
            @RequestParam Long userId) {
        try {
            GoalResponseDto goal = goalService.completeGoal(goalId, userId);
            return ResponseEntity.ok(goal);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/check-missed")
    public ResponseEntity<Void> checkMissedGoals() {
        goalService.markMissedGoals();
        return ResponseEntity.ok().build();
    }
}
