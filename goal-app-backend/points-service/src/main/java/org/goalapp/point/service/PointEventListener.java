package org.goalapp.point.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.goalapp.common.dto.NotificationEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class PointEventListener {

    @Autowired
    private PointsService pointsService;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @KafkaListener(topics = "points-topic", groupId = "points-service-group")
    public void handlePointsEvent(NotificationEvent event) {
        try {
            // Get goal details to determine difficulty
            String difficulty = getGoalDifficulty(event.getRelatedId());

            pointsService.addPoints(
                    event.getUserId(),
                    event.getRelatedId(),
                    event.getType(),
                    difficulty
            );

        } catch (Exception e) {
            System.err.println("Error processing points event: " + e.getMessage());
        }
    }

    private String getGoalDifficulty(Long goalId) {
        try {
            // Call goal service to get goal details
            return webClientBuilder.build()
                    .get()
                    .uri("http://localhost:8082/api/goals/{id}", goalId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(response -> {
                        try {
                            ObjectMapper mapper = new ObjectMapper();
                            var goalData = mapper.readTree(response);
                            return goalData.get("difficulty").asText();
                        } catch (Exception e) {
                            return "MEDIUM"; // default
                        }
                    })
                    .block();
        } catch (Exception e) {
            return "MEDIUM"; // default fallback
        }
    }
}
