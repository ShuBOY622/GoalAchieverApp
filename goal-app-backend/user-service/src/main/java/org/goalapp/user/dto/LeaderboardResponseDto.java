package org.goalapp.user.dto;

import java.util.List;

public class LeaderboardResponseDto {
    private List<LeaderboardUserDto> users;
    private UserRankDto currentUserRank;

    // Default constructor
    public LeaderboardResponseDto() {}

    // Constructor
    public LeaderboardResponseDto(List<LeaderboardUserDto> users, UserRankDto currentUserRank) {
        this.users = users;
        this.currentUserRank = currentUserRank;
    }

    // Getters and Setters
    public List<LeaderboardUserDto> getUsers() {
        return users;
    }

    public void setUsers(List<LeaderboardUserDto> users) {
        this.users = users;
    }

    public UserRankDto getCurrentUserRank() {
        return currentUserRank;
    }

    public void setCurrentUserRank(UserRankDto currentUserRank) {
        this.currentUserRank = currentUserRank;
    }
}