package org.goalapp.point.controller;

import org.goalapp.point.dto.PointsLogDto;
import org.goalapp.point.service.PointsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/points")
public class PointsControllers {

    @Autowired
    private PointsService pointsService;

    @PostMapping
    public ResponseEntity<PointsLogDto> addPoints(
            @RequestParam Long userId,
            @RequestParam Long goalId,
            @RequestParam String reason,
            @RequestParam(defaultValue = "MEDIUM") String difficulty) {
        try {
            PointsLogDto pointsLog = pointsService.addPoints(userId, goalId, reason, difficulty);
            return ResponseEntity.ok(pointsLog);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PointsLogDto>> getUserPointsHistory(@PathVariable Long userId) {
        List<PointsLogDto> history = pointsService.getUserPointsHistory(userId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/goal/{goalId}")
    public ResponseEntity<List<PointsLogDto>> getGoalPointsHistory(@PathVariable Long goalId) {
        List<PointsLogDto> history = pointsService.getGoalPointsHistory(goalId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/user/{userId}/total")
    public ResponseEntity<Integer> getTotalUserPoints(@PathVariable Long userId) {
        Integer total = pointsService.getTotalUserPoints(userId);
        return ResponseEntity.ok(total);
    }

    @GetMapping("/user/{userId}/period")
    public ResponseEntity<List<PointsLogDto>> getUserPointsInPeriod(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<PointsLogDto> history = pointsService.getUserPointsInPeriod(userId, start, end);
        return ResponseEntity.ok(history);
    }
}
