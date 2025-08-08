package org.goalapp.user.service;

import org.goalapp.common.dto.NotificationEvent;
import org.goalapp.user.dto.*;
import org.goalapp.user.entities.User;
import org.goalapp.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    public UserResponseDto registerUser(UserRegistrationDto registrationDto) {
        if (userRepository.existsByUsername(registrationDto.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setUsername(registrationDto.getUsername());
        user.setEmail(registrationDto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(registrationDto.getPassword()));

        User savedUser = userRepository.save(user);

        return convertToDto(savedUser);
    }

    public Optional<UserResponseDto> loginUser(UserLoginDto loginDto) {
        Optional<User> userOpt = userRepository.findByUsername(loginDto.getUsername());

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(loginDto.getPassword(), user.getPasswordHash())) {
                return Optional.of(convertToDto(user));
            }
        }
        return Optional.empty();
    }

    public Optional<UserResponseDto> getUserById(Long id) {
        return userRepository.findById(id).map(this::convertToDto);
    }

    public List<UserResponseDto> searchUsers(String query) {
        return userRepository.searchUsers(query).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public UserResponseDto updateUserPoints(Long userId, Integer pointsChange) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPoints(user.getPoints() + pointsChange);
        User savedUser = userRepository.save(user);

        return convertToDto(savedUser);
    }

    private UserResponseDto convertToDto(User user) {
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPoints(user.getPoints());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }

    public UserStatsDto getUserStats(Long userId) {
        // Calculate user statistics from database
        // This is a placeholder - implement based on your business logic

        UserStatsDto stats = new UserStatsDto();
        stats.setTotalGoals(0); // Calculate from goals table
        stats.setCompletedGoals(0); // Calculate completed goals
        stats.setTotalPoints(0); // Get from user points
        stats.setCurrentStreak(0); // Calculate streak
        stats.setPendingGoals(0); // Calculate pending goals
        stats.setOverdueGoals(0); // Calculate overdue goals

        return stats;
    }

    // Leaderboard methods
    public LeaderboardResponseDto getGlobalLeaderboard(int limit, String timeframe) {
        System.out.println("🏆 Getting global leaderboard with limit: " + limit + ", timeframe: " + timeframe);
        
        // Get all users ordered by points descending
        List<User> users = userRepository.findAll();
        System.out.println("📊 Found " + users.size() + " users in database");
        
        if (users.isEmpty()) {
            System.out.println("⚠️ No users found in database, returning empty leaderboard");
            return new LeaderboardResponseDto(List.of(), null);
        }
        
        users.sort((u1, u2) -> Integer.compare(u2.getPoints(), u1.getPoints()));
        
        // Log top users for debugging
        for (int i = 0; i < Math.min(3, users.size()); i++) {
            User user = users.get(i);
            System.out.println("🥇 User " + (i+1) + ": " + user.getUsername() + " with " + user.getPoints() + " points");
        }
        
        // Convert to leaderboard DTOs with ranks
        List<LeaderboardUserDto> leaderboardUsers = IntStream.range(0, Math.min(limit, users.size()))
                .mapToObj(i -> {
                    User user = users.get(i);
                    LeaderboardUserDto dto = new LeaderboardUserDto();
                    dto.setId(user.getId());
                    dto.setUsername(user.getUsername());
                    dto.setEmail(user.getEmail());
                    dto.setPoints(user.getPoints());
                    dto.setCreatedAt(user.getCreatedAt());
                    dto.setRank(i + 1);
                    dto.setCompletedGoals(0); // TODO: Calculate from goals service
                    dto.setStreak(0); // TODO: Calculate streak
                    return dto;
                })
                .collect(Collectors.toList());

        System.out.println("✅ Returning " + leaderboardUsers.size() + " users in leaderboard");
        return new LeaderboardResponseDto(leaderboardUsers, null);
    }

    public List<LeaderboardUserDto> getFriendsLeaderboard(Long userId, int limit) {
        // TODO: Implement friends leaderboard by getting user's friends
        // For now, return empty list
        return List.of();
    }

    public UserRankDto getUserRank(Long userId) {
        List<User> allUsers = userRepository.findAll();
        allUsers.sort((u1, u2) -> Integer.compare(u2.getPoints(), u1.getPoints()));
        
        int rank = 1;
        for (int i = 0; i < allUsers.size(); i++) {
            if (allUsers.get(i).getId().equals(userId)) {
                rank = i + 1;
                break;
            }
        }
        
        int totalUsers = allUsers.size();
        double percentile = totalUsers > 0 ? ((double) (totalUsers - rank + 1) / totalUsers) * 100 : 0;
        
        return new UserRankDto(rank, totalUsers, percentile);
    }

    public List<LeaderboardUserDto> getTopPerformers(int limit, String timeframe) {
        System.out.println("🏅 Getting top performers with limit: " + limit + ", timeframe: " + timeframe);
        
        List<User> users = userRepository.findAll();
        System.out.println("📊 Found " + users.size() + " users for top performers");
        
        if (users.isEmpty()) {
            System.out.println("⚠️ No users found for top performers, returning empty list");
            return List.of();
        }
        
        users.sort((u1, u2) -> Integer.compare(u2.getPoints(), u1.getPoints()));
        
        List<LeaderboardUserDto> topPerformers = IntStream.range(0, Math.min(limit, users.size()))
                .mapToObj(i -> {
                    User user = users.get(i);
                    LeaderboardUserDto dto = new LeaderboardUserDto();
                    dto.setId(user.getId());
                    dto.setUsername(user.getUsername());
                    dto.setEmail(user.getEmail());
                    dto.setPoints(user.getPoints());
                    dto.setCreatedAt(user.getCreatedAt());
                    dto.setRank(i + 1);
                    dto.setCompletedGoals(0); // TODO: Calculate from goals service
                    dto.setStreak(0); // TODO: Calculate streak
                    System.out.println("🏆 Top performer " + (i+1) + ": " + user.getUsername() + " (" + user.getPoints() + " points)");
                    return dto;
                })
                .collect(Collectors.toList());
        
        System.out.println("✅ Returning " + topPerformers.size() + " top performers");
        return topPerformers;
    }
}
