package org.goalapp.challenge.service;

import org.goalapp.challenge.client.UserClient;
import org.goalapp.challenge.dto.ChallengeResponse;
import org.goalapp.challenge.dto.CreateChallengeRequest;
import org.goalapp.challenge.dto.UserDto;
import org.goalapp.challenge.entities.Challenge;
import org.goalapp.challenge.repository.ChallengeRepository;
import org.goalapp.common.dto.GoalCreatedEvent;
import org.goalapp.common.dto.NotificationEvent;
import org.goalapp.common.dto.CreateSharedGoalEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChallengeService {

    private static final Logger log = LoggerFactory.getLogger(ChallengeService.class);

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private KafkaTemplate<String, NotificationEvent> notificationKafkaTemplate;

    @Autowired
    private KafkaTemplate<String, CreateSharedGoalEvent> goalKafkaTemplate;

    @Autowired
    private UserClient userClient;

    private Challenge.ChallengeStatus mapResponseToStatus(ChallengeResponse response) {
        switch (response) {
            case ACCEPT:
                return Challenge.ChallengeStatus.ACCEPTED;
            case REJECT:
                return Challenge.ChallengeStatus.REJECTED;
            default:
                throw new IllegalArgumentException("Invalid challenge response: " + response);
        }
    }

    /**
     * Send a challenge from one user to another
     */
    @Transactional
    public Challenge sendChallenge(CreateChallengeRequest request) {
        log.info("Sending challenge from user {} to user {}", request.getChallengerId(), request.getChallengedUserId());

        // Validate request
        validateChallengeRequest(request);

        // Create challenge entity
        Challenge challenge = new Challenge();
        challenge.setChallengerId(request.getChallengerId());
        challenge.setChallengedUserId(request.getChallengedUserId());
        challenge.setTitle(request.getTitle());
        challenge.setDescription(request.getDescription());
        challenge.setDifficulty(Challenge.ChallengeDifficulty.valueOf(request.getDifficulty().toString().toUpperCase()));
        challenge.setDeadline(request.getDeadline());
        challenge.setStatus(Challenge.ChallengeStatus.PENDING);
        challenge.setCreatedAt(LocalDateTime.now());

        Challenge savedChallenge = challengeRepository.save(challenge);
        log.info("Challenge created with ID: {}", savedChallenge.getId());

        // Send notification to challenged user
        sendChallengeNotification(savedChallenge);

        return savedChallenge;
    }

    /**
     * Respond to a challenge (accept or reject)
     */
    @Transactional
    public Challenge respondToChallenge(Long challengeId, Long userId, ChallengeResponse response) {
        try {
            Challenge challenge = challengeRepository.findById(challengeId)
                    .orElseThrow(() -> new RuntimeException("Challenge not found"));

            // Verify user is the challenged user
            if (!challenge.getChallengedUserId().equals(userId)) {
                throw new RuntimeException("User not authorized to respond to this challenge");
            }

            // Verify challenge is still pending
            if (challenge.getStatus() != Challenge.ChallengeStatus.PENDING) {
                throw new RuntimeException("Challenge has already been responded to");
            }

            // Map and set status
            Challenge.ChallengeStatus newStatus = mapResponseToStatus(response);
            challenge.setStatus(newStatus);
            challenge.setRespondedAt(LocalDateTime.now());

            Challenge savedChallenge = challengeRepository.save(challenge);

            log.info("✅ Challenge {} {} by user {} - Status updated to {}",
                    challengeId, response, userId, newStatus);

            // Send notification to challenger about the response
            sendChallengerResponseNotification(savedChallenge, response);

            if (response == ChallengeResponse.ACCEPT) {
                log.info("🎯 Challenge accepted - Creating shared goal via Kafka");
                createSharedGoalViaKafka(savedChallenge);
            }

            // ✅ Return the updated challenge
            return savedChallenge;

        } catch (Exception e) {
            log.error("❌ Failed to respond to challenge {}: {}", challengeId, e.getMessage(), e);
            throw e;
        }
    }
    // ... rest of your methods

    private void sendChallengerResponseNotification(Challenge challenge, ChallengeResponse response) {
        try {
            // Get the username of the user who responded to the challenge
            String respondingUsername = "Unknown User"; // Default fallback
            try {
                UserDto respondingUser = userClient.getUserById(challenge.getChallengedUserId());
                if (respondingUser != null && respondingUser.getUsername() != null) {
                    respondingUsername = respondingUser.getUsername();
                }
            } catch (Exception e) {
                log.warn("Failed to fetch username for user {}: {}", challenge.getChallengedUserId(), e.getMessage());
            }

            String message = response == ChallengeResponse.ACCEPT
                    ? String.format("🎉 Great news! %s accepted your challenge '%s'! A shared goal has been created.", respondingUsername, challenge.getTitle())
                    : String.format("😔 %s declined your challenge '%s'.", respondingUsername, challenge.getTitle());

            String notificationType = "CHALLENGE_RESPONSE";

            NotificationEvent notification = new NotificationEvent(
                    challenge.getChallengerId(),     // Notify the challenger
                    notificationType,                // Type of notification
                    message,                         // User-friendly message
                    challenge.getChallengedUserId(), // Who responded (source user)
                    challenge.getId()                // Related challenge ID
            );

            log.info("📤 Sending {} notification to challenger (user {}): {}",
                    notificationType, challenge.getChallengerId(), message);

            notificationKafkaTemplate.send("notification-topic", notification);

        } catch (Exception e) {
            log.error("❌ Failed to send challenger response notification", e);
        }
    }

    /**
     * Get pending challenges for a user
     */
    public List<Challenge> getPendingChallenges(Long userId) {
        log.debug("Getting pending challenges for user: {}", userId);
        return challengeRepository.findByChallengedUserIdAndStatus(userId, Challenge.ChallengeStatus.PENDING);
    }

    /**
     * Get challenges sent by a user
     */
    public List<Challenge> getSentChallenges(Long userId) {
        log.debug("Getting sent challenges for user: {}", userId);
        return challengeRepository.findByChallengerIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Validate challenge request
     */
    private void validateChallengeRequest(CreateChallengeRequest request) {
        if (request.getChallengerId().equals(request.getChallengedUserId())) {
            throw new RuntimeException("Cannot challenge yourself");
        }

        if (request.getDeadline().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new RuntimeException("Challenge deadline must be at least 1 hour in the future");
        }

        // Add additional validation as needed (e.g., check if users are friends)
    }

    /**
     * Send challenge notification to challenged user
     */
    private void sendChallengeNotification(Challenge challenge) {
        try {
            // Get the username of the challenger
            String challengerUsername = "Unknown User"; // Default fallback
            try {
                UserDto challenger = userClient.getUserById(challenge.getChallengerId());
                if (challenger != null && challenger.getUsername() != null) {
                    challengerUsername = challenger.getUsername();
                }
            } catch (Exception e) {
                log.warn("Failed to fetch username for challenger {}: {}", challenge.getChallengerId(), e.getMessage());
            }

            // ✅ Create proper NotificationEvent with all required fields
            NotificationEvent notification = new NotificationEvent(
                    challenge.getChallengedUserId(),
                    "CHALLENGE_RECEIVED",
                    String.format("🎯 %s challenged you to: %s", challengerUsername, challenge.getTitle()),
                    challenge.getChallengerId(),
                    challenge.getId()
            );

            log.info("Sending challenge notification: {}", notification);

            // Send to notification topic
            notificationKafkaTemplate.send("notification-topic", notification);

            log.debug("Successfully sent challenge notification for challenge: {}", challenge.getId());
        } catch (Exception e) {
            log.error("Failed to send challenge notification", e);
        }
    }

    /**
     * Create shared goal via Kafka when challenge is accepted
     */
    private void createSharedGoalViaKafka(Challenge challenge) {
        try {
            CreateSharedGoalEvent event = new CreateSharedGoalEvent();
            event.setChallengeId(challenge.getId());
            event.setTitle(challenge.getTitle());
            event.setDescription(challenge.getDescription());
            event.setChallengerId(challenge.getChallengerId());
            event.setChallengedUserId(challenge.getChallengedUserId());
            event.setDifficulty(challenge.getDifficulty().toString());
            event.setDeadline(challenge.getDeadline());

            // ✅ Add logging before sending
            log.info("📤 Sending create shared goal event: challengeId={}, title={}, users=[{}, {}]",
                    event.getChallengeId(), event.getTitle(),
                    event.getChallengerId(), event.getChallengedUserId());

            goalKafkaTemplate.send("create-shared-goal-topic", event);

            log.info("✅ Successfully sent create shared goal event for challenge: {}",
                    challenge.getId());

        } catch (Exception e) {
            log.error("❌ Failed to send create shared goal event for challenge: {}",
                    challenge.getId(), e);
        }
    }

    /**
     * Handle goal creation response from goal service
     */
    @KafkaListener(topics = "goal-created-topic", groupId = "challenge-service-group")
    public void handleGoalCreated(GoalCreatedEvent event) {
        try {
            Challenge challenge = challengeRepository.findById(event.getChallengeId())
                    .orElseThrow(() -> new RuntimeException("Challenge not found"));

            if (event.isSuccess()) {
                challenge.setSharedGoalId(event.getGoalId());
                challengeRepository.save(challenge);
                log.info("Updated challenge {} with shared goal ID: {}",
                        challenge.getId(), event.getGoalId());
            } else {
                log.error("Failed to create shared goal for challenge {}: {}",
                        challenge.getId(), event.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("Error handling goal created event", e);
        }
    }
}
