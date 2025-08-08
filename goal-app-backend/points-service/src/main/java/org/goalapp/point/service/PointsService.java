package org.goalapp.point.service;

import org.goalapp.common.dto.NotificationEvent;
import org.goalapp.point.dto.PointsLogDto;
import org.goalapp.point.entities.PointsLog;
import org.goalapp.point.repository.PointsLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PointsService {

    @Autowired
    private PointsLogRepository pointsLogRepository;

    @Autowired
    private KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @Autowired
    private WebClient.Builder webClientBuilder;

    public PointsLogDto addPoints(Long userId, Long goalId, String reason, String difficulty) {
        int pointsChange = calculatePoints(reason, difficulty);

        PointsLog pointsLog = new PointsLog();
        pointsLog.setUserId(userId);
        pointsLog.setGoalId(goalId);
        pointsLog.setPointsChange(pointsChange);
        pointsLog.setReason(reason);

        PointsLog savedLog = pointsLogRepository.save(pointsLog);

        // Update user points via user service
        updateUserPoints(userId, pointsChange);

        return convertToDto(savedLog);
    }

    public List<PointsLogDto> getUserPointsHistory(Long userId) {
        return pointsLogRepository.findByUserId(userId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<PointsLogDto> getGoalPointsHistory(Long goalId) {
        return pointsLogRepository.findByGoalId(goalId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Integer getTotalUserPoints(Long userId) {
        Integer total = pointsLogRepository.getTotalPointsByUserId(userId);
        return total != null ? total : 0;
    }

    public List<PointsLogDto> getUserPointsInPeriod(Long userId, LocalDateTime start, LocalDateTime end) {
        return pointsLogRepository.findByUserIdAndLoggedAtBetween(userId, start, end).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private int calculatePoints(String reason, String difficulty) {
        int basePoints = 0;

        if ("GOAL_COMPLETED".equals(reason)) {
            switch (difficulty) {
                case "EASY": basePoints = 10; break;
                case "MEDIUM": basePoints = 20; break;
                case "HARD": basePoints = 30; break;
                default: basePoints = 15;
            }
        } else if ("GOAL_MISSED".equals(reason)) {
            switch (difficulty) {
                case "EASY": basePoints = -5; break;
                case "MEDIUM": basePoints = -10; break;
                case "HARD": basePoints = -15; break;
                default: basePoints = -10;
            }
        }

        return basePoints;
    }

    private void updateUserPoints(Long userId, Integer pointsChange) {
        try {
            webClientBuilder.build()
                    .put()
                    .uri("http://localhost:8081/api/users/{id}/points?pointsChange={pointsChange}",
                            userId, pointsChange)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            System.err.println("Failed to update user points: " + e.getMessage());
        }
    }

    private PointsLogDto convertToDto(PointsLog pointsLog) {
        PointsLogDto dto = new PointsLogDto();
        dto.setId(pointsLog.getId());
        dto.setUserId(pointsLog.getUserId());
        dto.setGoalId(pointsLog.getGoalId());
        dto.setPointsChange(pointsLog.getPointsChange());
        dto.setReason(pointsLog.getReason());
        dto.setLoggedAt(pointsLog.getLoggedAt());
        return dto;
    }
}
