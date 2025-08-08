package org.goalapp.challenge.dto;

import jakarta.validation.constraints.*;
import org.goalapp.challenge.entities.Challenge;

import java.time.LocalDateTime;

public class CreateChallengeRequest {
    @NotNull
    private Long challengerId;

    @NotNull
    private Long challengedUserId;

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotNull
    @Future
    private LocalDateTime deadline;


    @Size(max = 1000)
    private String description;

    @NotNull
    private Challenge.ChallengeDifficulty difficulty;

    public Long getChallengedUserId() {
        return challengedUserId;
    }

    public void setChallengedUserId(Long challengedUserId) {
        this.challengedUserId = challengedUserId;
    }

    public Long getChallengerId() {
        return challengerId;
    }

    public void setChallengerId(Long challengerId) {
        this.challengerId = challengerId;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Challenge.ChallengeDifficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Challenge.ChallengeDifficulty difficulty) {
        this.difficulty = difficulty;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }


    // Getters and setters
}

