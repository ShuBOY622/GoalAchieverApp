package org.goalapp.goal.kafka;

import org.goalapp.common.dto.CreateSharedGoalEvent;
import org.goalapp.common.dto.GoalCreatedEvent;
import org.goalapp.goal.dto.GoalCreateDto; // ✅ Use your existing DTO
import org.goalapp.goal.entities.Goal;
import org.goalapp.goal.service.GoalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

@Service
public class ChallengeEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ChallengeEventConsumer.class);

    @Autowired
    private GoalService goalService;

    @Autowired
    private KafkaTemplate<String, GoalCreatedEvent> kafkaTemplate;

    @KafkaListener(topics = "create-shared-goal-topic", groupId = "goal-service-group")
    public void handleCreateSharedGoal(CreateSharedGoalEvent event) {
        try {
            // ✅ Add detailed logging
            log.info("📨 Received create shared goal event: challengeId={}, title={}, challengerId={}, challengedUserId={}",
                    event.getChallengeId(), event.getTitle(), event.getChallengerId(), event.getChallengedUserId());

            // ✅ Map event to DTO with logging
            GoalCreateDto goalCreateDto = new GoalCreateDto();
            goalCreateDto.setTitle(event.getTitle());
            goalCreateDto.setDescription(event.getDescription());
            goalCreateDto.setCreatedBy(event.getChallengerId());
            goalCreateDto.setType("SHARED");
            goalCreateDto.setDifficulty(event.getDifficulty());
            goalCreateDto.setDeadline(event.getDeadline());

            // ✅ Log assigned users
            List<Long> assignedUsers = Arrays.asList(
                    event.getChallengerId(),
                    event.getChallengedUserId()
            );
            goalCreateDto.setAssignedUserIds(assignedUsers);

            log.info("🎯 Creating shared goal with DTO: title={}, type={}, difficulty={}, assignedUsers={}",
                    goalCreateDto.getTitle(), goalCreateDto.getType(),
                    goalCreateDto.getDifficulty(), assignedUsers);

            // ✅ Call service with try-catch
            Goal sharedGoal = goalService.createGoal(goalCreateDto);

            // ✅ Verify goal was created
            if (sharedGoal != null && sharedGoal.getId() != null) {
                log.info("✅ Successfully created shared goal with ID: {} for challenge: {}",
                        sharedGoal.getId(), event.getChallengeId());

                // Send success response
                GoalCreatedEvent response = new GoalCreatedEvent();
                response.setChallengeId(event.getChallengeId());
                response.setGoalId(sharedGoal.getId());
                response.setSuccess(true);
                kafkaTemplate.send("goal-created-topic", response);

            } else {
                log.error("❌ Goal creation returned null or invalid goal for challenge: {}",
                        event.getChallengeId());
                throw new RuntimeException("Goal creation failed - null result");
            }

        } catch (Exception e) {
            log.error("❌ Failed to create shared goal for challenge {}: {}",
                    event.getChallengeId(), e.getMessage(), e);

            // Send failure response
            GoalCreatedEvent response = new GoalCreatedEvent();
            response.setChallengeId(event.getChallengeId());
            response.setSuccess(false);
            response.setErrorMessage(e.getMessage());
            kafkaTemplate.send("goal-created-topic", response);
        }
    }
}
