package org.goalapp.common.dto;

public class GoalCreatedEvent {
    private Long challengeId;
    private Long goalId;
    private boolean success;
    private String errorMessage;

    // Default constructor
    public GoalCreatedEvent() {}

    // Getters and setters
    public Long getChallengeId() { return challengeId; }
    public void setChallengeId(Long challengeId) { this.challengeId = challengeId; }

    public Long getGoalId() { return goalId; }
    public void setGoalId(Long goalId) { this.goalId = goalId; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
