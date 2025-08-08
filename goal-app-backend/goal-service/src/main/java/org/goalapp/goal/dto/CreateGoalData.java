package org.goalapp.goal.dto;


import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public class CreateGoalData {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotBlank(message = "Difficulty is required")
    private String difficulty; // "EASY", "MEDIUM", "HARD"

    @NotNull(message = "Deadline is required")
    @Future(message = "Deadline must be in the future")
    private LocalDateTime deadline;

    private String type = "PERSONAL"; // "PERSONAL", "SHARED", "CHALLENGE"

    @NotNull(message = "Created by user ID is required")
    private Long createdBy;

    private List<Long> assignedUsers; // For shared goals

    private String category; // Optional category

    private Integer priority = 1; // 1-5 priority scale

    private boolean isPublic = false;

    // Default constructor
    public CreateGoalData() {}

    // Constructor with essential fields
    public CreateGoalData(String title, String description, String difficulty,
                          LocalDateTime deadline, Long createdBy) {
        this.title = title;
        this.description = description;
        this.difficulty = difficulty;
        this.deadline = deadline;
        this.createdBy = createdBy;
    }

    // Getters and setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public List<Long> getAssignedUsers() {
        return assignedUsers;
    }

    public void setAssignedUsers(List<Long> assignedUsers) {
        this.assignedUsers = assignedUsers;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    @Override
    public String toString() {
        return "CreateGoalData{" +
                "title='" + title + '\'' +
                ", difficulty='" + difficulty + '\'' +
                ", deadline=" + deadline +
                ", type='" + type + '\'' +
                ", createdBy=" + createdBy +
                ", assignedUsers=" + assignedUsers +
                '}';
    }
}
