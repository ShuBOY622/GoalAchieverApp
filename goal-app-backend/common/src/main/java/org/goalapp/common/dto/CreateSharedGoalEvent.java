package org.goalapp.common.dto;

import java.time.LocalDateTime;

public class CreateSharedGoalEvent {
    private Long challengeId;
    private String title;
    private String description;
    private String difficulty;
    private LocalDateTime deadline;
    private Long challengerId;
    private Long challengedUserId;

    // Default constructor
    public CreateSharedGoalEvent() {}

    // Getters and setters
    public Long getChallengeId() { return challengeId; }
    public void setChallengeId(Long challengeId) { this.challengeId = challengeId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }

    public Long getChallengerId() { return challengerId; }
    public void setChallengerId(Long challengerId) { this.challengerId = challengerId; }

    public Long getChallengedUserId() { return challengedUserId; }
    public void setChallengedUserId(Long challengedUserId) { this.challengedUserId = challengedUserId; }
}
