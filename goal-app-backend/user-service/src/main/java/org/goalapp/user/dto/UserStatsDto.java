package org.goalapp.user.dto;


public class UserStatsDto {
    private int totalGoals;
    private int completedGoals;
    private int totalPoints;
    private int currentStreak;
    private int pendingGoals;
    private int overdueGoals;

    // Constructors
    public UserStatsDto() {}

    public UserStatsDto(int totalGoals, int completedGoals, int totalPoints,
                        int currentStreak, int pendingGoals, int overdueGoals) {
        this.totalGoals = totalGoals;
        this.completedGoals = completedGoals;
        this.totalPoints = totalPoints;
        this.currentStreak = currentStreak;
        this.pendingGoals = pendingGoals;
        this.overdueGoals = overdueGoals;
    }

    // Getters and Setters
    public int getTotalGoals() { return totalGoals; }
    public void setTotalGoals(int totalGoals) { this.totalGoals = totalGoals; }

    public int getCompletedGoals() { return completedGoals; }
    public void setCompletedGoals(int completedGoals) { this.completedGoals = completedGoals; }

    public int getTotalPoints() { return totalPoints; }
    public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }

    public int getPendingGoals() { return pendingGoals; }
    public void setPendingGoals(int pendingGoals) { this.pendingGoals = pendingGoals; }

    public int getOverdueGoals() { return overdueGoals; }
    public void setOverdueGoals(int overdueGoals) { this.overdueGoals = overdueGoals; }
}
