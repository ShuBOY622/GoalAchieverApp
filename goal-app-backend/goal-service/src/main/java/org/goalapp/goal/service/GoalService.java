package org.goalapp.goal.service;

import org.goalapp.common.dto.NotificationEvent;
import org.goalapp.goal.client.UserClient;
import org.goalapp.goal.dto.GoalCreateDto;
import org.goalapp.goal.dto.GoalResponseDto;
import org.goalapp.goal.dto.UserDto;
import org.goalapp.goal.entities.Goal;
import org.goalapp.goal.entities.GoalAssignment;
import org.goalapp.goal.kafka.ChallengeEventConsumer;
import org.goalapp.goal.repository.GoalRepository;
import org.goalapp.goal.repository.GoalAssignmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class GoalService {

    private static final Logger logger = LoggerFactory.getLogger(GoalService.class);

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private GoalAssignmentRepository goalAssignmentRepository;

    @Autowired
    private KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @Autowired
    private UserClient userClient;

    /**
     * Core method to create Goal entity and save it.
     * This method handles the business logic and returns the Goal entity.
     * Used internally and by Kafka consumers.
     */
    private static final Logger log = LoggerFactory.getLogger(ChallengeEventConsumer.class);

    @Transactional
    public Goal createGoal(@Valid GoalCreateDto goalCreateDto) {

        // ✅ Add detailed logging at start
        log.info("🚀 Starting goal creation: title={}, type={}, createdBy={}, assignedUsers={}",
                goalCreateDto.getTitle(), goalCreateDto.getType(),
                goalCreateDto.getCreatedBy(), goalCreateDto.getAssignedUserIds());

        try {
            Goal goal = new Goal();
            goal.setTitle(goalCreateDto.getTitle());
            goal.setDescription(goalCreateDto.getDescription());
            goal.setCreatedBy(goalCreateDto.getCreatedBy());
            goal.setType(Goal.GoalType.valueOf(goalCreateDto.getType().toUpperCase()));
            goal.setDifficulty(Goal.Difficulty.valueOf(goalCreateDto.getDifficulty().toUpperCase()));
            goal.setDeadline(goalCreateDto.getDeadline());

            // ✅ Log before saving
            log.info("💾 Saving goal entity: {}", goal);
            Goal savedGoal = goalRepository.save(goal);

            // ✅ Verify save succeeded
            if (savedGoal != null && savedGoal.getId() != null) {
                log.info("✅ Successfully saved goal with ID: {} and type: {}",
                        savedGoal.getId(), savedGoal.getType());
            } else {
                log.error("❌ Goal save failed - returned null or no ID");
                throw new RuntimeException("Goal save failed");
            }

            // ✅ Log assignment creation
            log.info("👥 Creating goal assignments for {} users",
                    goalCreateDto.getAssignedUserIds() != null ? goalCreateDto.getAssignedUserIds().size() : 0);

            createGoalAssignments(savedGoal, goalCreateDto);

            log.info("✅ Goal creation completed successfully with ID: {}", savedGoal.getId());
            return savedGoal;

        } catch (Exception e) {
            log.error("❌ Goal creation failed: {}", e.getMessage(), e);
            throw e; // Re-throw to trigger transaction rollback
        }
    }

    /**
     * Public API method to create goal and return DTO.
     * Used by REST controllers.
     */
    @Transactional
    public GoalResponseDto createGoalDto(@Valid GoalCreateDto goalCreateDto) {
        Goal savedGoal = createGoal(goalCreateDto);
        return convertToDto(savedGoal);
    }

    private void createGoalAssignments(Goal savedGoal, GoalCreateDto goalCreateDto) {
        List<Long> userIds = goalCreateDto.getAssignedUserIds();

        log.info("📝 Creating assignments for goal ID: {} to users: {}",
                savedGoal.getId(), userIds);

        if (userIds != null && !userIds.isEmpty()) {
            for (Long userId : userIds) {
                log.info("👤 Creating assignment for user: {} to goal: {}", userId, savedGoal.getId());
                createGoalAssignment(savedGoal.getId(), userId);

                // Send notification to assigned users (except creator)
                if (!userId.equals(goalCreateDto.getCreatedBy())) {
                    log.info("📧 Sending notification to user: {} for goal: {}", userId, savedGoal.getId());
                    sendGoalAssignmentNotification(userId, savedGoal);
                }
            }
            log.info("✅ Created {} assignments for goal: {}", userIds.size(), savedGoal.getId());
        } else {
            log.warn("⚠️ No assigned users found, assigning to creator: {}", goalCreateDto.getCreatedBy());
            createGoalAssignment(savedGoal.getId(), goalCreateDto.getCreatedBy());
        }
    }

    private void createGoalAssignment(Long goalId, Long userId) {
        log.info("💼 Creating assignment: goalId={}, userId={}", goalId, userId);

        GoalAssignment assignment = new GoalAssignment();
        assignment.setGoalId(goalId);
        assignment.setUserId(userId);
        assignment.setStatus(GoalAssignment.Status.PENDING);

        GoalAssignment savedAssignment = goalAssignmentRepository.save(assignment);

        if (savedAssignment != null && savedAssignment.getId() != null) {
            log.info("✅ Assignment created with ID: {} for user: {} and goal: {}",
                    savedAssignment.getId(), userId, goalId);
        } else {
            log.error("❌ Assignment creation failed for user: {} and goal: {}", userId, goalId);
        }
    }

    /**
     * Helper method to send goal assignment notification
     */
    private void sendGoalAssignmentNotification(Long userId, Goal goal) {
        try {
            NotificationEvent notification = new NotificationEvent(
                    userId,
                    "GOAL_ASSIGNED",
                    "You have been assigned a new goal: " + goal.getTitle(),
                    goal.getCreatedBy(),  // sourceUserId - who created/assigned the goal
                    goal.getId()          // relatedId - the goal that was assigned
            );
            kafkaTemplate.send("notification-topic", notification);
            logger.debug("Sent goal assignment notification to user: {}", userId);
        } catch (Exception e) {
            logger.error("Failed to send goal assignment notification to user: {}", userId, e);
        }
    }

    public Optional<GoalResponseDto> getGoalById(Long id) {
        return goalRepository.findById(id).map(this::convertToDto);
    }

    public List<GoalResponseDto> getGoalsByUserId(Long userId) {
        return goalRepository.findGoalsByUserId(userId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<GoalResponseDto> getGoalsByCreator(Long creatorId) {
        return goalRepository.findByCreatedBy(creatorId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<GoalResponseDto> searchGoals(String query) {
        return goalRepository.searchGoals(query).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public GoalResponseDto completeGoal(Long goalId, Long userId) {
        logger.info("Completing goal {} for user {}", goalId, userId);

        Optional<GoalAssignment> assignmentOpt = goalAssignmentRepository.findByGoalIdAndUserId(goalId, userId);

        if (assignmentOpt.isEmpty()) {
            throw new RuntimeException("Goal assignment not found for user " + userId + " and goal " + goalId);
        }

        GoalAssignment assignment = assignmentOpt.get();
        if (assignment.getStatus() == GoalAssignment.Status.COMPLETED) {
            throw new RuntimeException("Goal already completed");
        }

        // Update assignment status
        assignment.setStatus(GoalAssignment.Status.COMPLETED);
        assignment.setCompletedAt(LocalDateTime.now());
        assignment.setLastUpdated(LocalDateTime.now());
        goalAssignmentRepository.save(assignment);

        // Get goal details
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        // Send notification to points service for point calculation
        sendPointsNotification(userId, goal, "GOAL_COMPLETED");

        // Notify other assigned users about completion
        notifyOtherAssignedUsers(goalId, userId, goal);

        logger.info("Successfully completed goal {} for user {}", goalId, userId);
        return convertToDto(goal);
    }

    /**
     * Helper method to send points notification
     */
    private void sendPointsNotification(Long userId, Goal goal, String eventType) {
        try {
            NotificationEvent pointsEvent = new NotificationEvent(
                    userId,
                    eventType,
                    eventType.equals("GOAL_COMPLETED") ? "Goal completed: " + goal.getTitle() : "Goal missed: " + goal.getTitle(),
                    userId,        // sourceUserId - the user who completed/missed the goal
                    goal.getId()   // relatedId - the goal
            );
            kafkaTemplate.send("points-topic", pointsEvent);
        } catch (Exception e) {
            logger.error("Failed to send points notification for user: {}", userId, e);
        }
    }

    /**
     * Helper method to notify other assigned users
     */
    private void notifyOtherAssignedUsers(Long goalId, Long completingUserId, Goal goal) {
        try {
            List<GoalAssignment> otherAssignments = goalAssignmentRepository.findByGoalId(goalId);
            
            // Get the username of the user who completed the goal
            String completingUsername = "Unknown User"; // Default fallback
            try {
                UserDto completingUser = userClient.getUserById(completingUserId);
                if (completingUser != null && completingUser.getUsername() != null) {
                    completingUsername = completingUser.getUsername();
                }
            } catch (Exception e) {
                logger.warn("Failed to fetch username for user {}: {}", completingUserId, e.getMessage());
            }
            
            for (GoalAssignment otherAssignment : otherAssignments) {
                if (!otherAssignment.getUserId().equals(completingUserId)) {
                    NotificationEvent notification = new NotificationEvent(
                            otherAssignment.getUserId(),
                            "GOAL_COMPLETED_BY_FRIEND",
                            completingUsername + " completed the goal: " + goal.getTitle(),
                            completingUserId,  // sourceUserId - who completed the goal
                            goalId             // relatedId - the goal that was completed
                    );
                    kafkaTemplate.send("notification-topic", notification);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to notify other users about goal completion", e);
        }
    }

    @Transactional
    public void markMissedGoals() {
        logger.info("Starting to mark missed goals");

        LocalDateTime now = LocalDateTime.now();
        List<Goal> expiredGoals = goalRepository.findByDeadlineBefore(now);

        int missedCount = 0;

        for (Goal goal : expiredGoals) {
            List<GoalAssignment> assignments = goalAssignmentRepository.findByGoalId(goal.getId());

            for (GoalAssignment assignment : assignments) {
                if (assignment.getStatus() == GoalAssignment.Status.PENDING) {
                    assignment.setStatus(GoalAssignment.Status.MISSED);
                    assignment.setLastUpdated(now);
                    goalAssignmentRepository.save(assignment);

                    // Send notification to points service for penalty
                    sendPointsNotification(assignment.getUserId(), goal, "GOAL_MISSED");

                    missedCount++;
                }
            }
        }

        logger.info("Marked {} goal assignments as missed", missedCount);
    }

    /**
     * Convert Goal entity to DTO
     */
    private GoalResponseDto convertToDto(Goal goal) {
        GoalResponseDto dto = new GoalResponseDto();
        dto.setId(goal.getId());
        dto.setTitle(goal.getTitle());
        dto.setDescription(goal.getDescription());
        dto.setCreatedBy(goal.getCreatedBy());
        dto.setType(goal.getType().toString());
        dto.setDifficulty(goal.getDifficulty().toString());
        dto.setDeadline(goal.getDeadline());
        dto.setCreatedAt(goal.getCreatedAt());

        // Add assignment details
        List<GoalAssignment> assignments = goalAssignmentRepository.findByGoalId(goal.getId());
        dto.setAssignments(assignments.stream()
                .map(this::convertAssignmentToDto)
                .collect(Collectors.toList()));

        return dto;
    }

    /**
     * Convert GoalAssignment entity to DTO
     */
    private GoalResponseDto.AssignmentDto convertAssignmentToDto(GoalAssignment assignment) {
        GoalResponseDto.AssignmentDto dto = new GoalResponseDto.AssignmentDto();
        dto.setUserId(assignment.getUserId());
        dto.setStatus(assignment.getStatus().toString());
        dto.setCompletedAt(assignment.getCompletedAt());
        dto.setLastUpdated(assignment.getLastUpdated());
        return dto;
    }
}
