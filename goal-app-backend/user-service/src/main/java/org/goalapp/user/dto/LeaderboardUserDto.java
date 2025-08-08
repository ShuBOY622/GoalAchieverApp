package org.goalapp.user.dto;

import java.time.LocalDateTime;

public class LeaderboardUserDto {
    private Long id;
    private String username;
    private String email;
    private Integer points;
    private LocalDateTime createdAt;
    private Integer rank;
    private Integer completedGoals;
    private Integer streak;

    // Default constructor
    public LeaderboardUserDto() {}

    // Constructor
    public LeaderboardUserDto(Long id, String username, String email, Integer points, 
                             LocalDateTime createdAt, Integer rank, Integer completedGoals, Integer streak) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.points = points;
        this.createdAt = createdAt;
        this.rank = rank;
        this.completedGoals = completedGoals;
        this.streak = streak;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public Integer getCompletedGoals() {
        return completedGoals;
    }

    public void setCompletedGoals(Integer completedGoals) {
        this.completedGoals = completedGoals;
    }

    public Integer getStreak() {
        return streak;
    }

    public void setStreak(Integer streak) {
        this.streak = streak;
    }
}