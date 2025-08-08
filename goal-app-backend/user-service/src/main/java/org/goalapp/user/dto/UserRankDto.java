package org.goalapp.user.dto;

public class UserRankDto {
    private Integer rank;
    private Integer totalUsers;
    private Double percentile;

    // Default constructor
    public UserRankDto() {}

    // Constructor
    public UserRankDto(Integer rank, Integer totalUsers, Double percentile) {
        this.rank = rank;
        this.totalUsers = totalUsers;
        this.percentile = percentile;
    }

    // Getters and Setters
    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public Integer getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(Integer totalUsers) {
        this.totalUsers = totalUsers;
    }

    public Double getPercentile() {
        return percentile;
    }

    public void setPercentile(Double percentile) {
        this.percentile = percentile;
    }
}