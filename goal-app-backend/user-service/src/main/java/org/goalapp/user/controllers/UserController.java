package org.goalapp.user.controllers;

import org.goalapp.user.dto.*;
import org.goalapp.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> register(@Valid @RequestBody UserRegistrationDto registrationDto) {
        System.out.println("Recieved : "+registrationDto.getEmail() + registrationDto.getPassword()+registrationDto.getUsername());
        try {
            UserResponseDto user = userService.registerUser(registrationDto);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getCurrentUser(@RequestParam Long id) {
        Optional<UserResponseDto> user = userService.getUserById(id);
        return user.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }



    @GetMapping("/{id}/stats")
    public ResponseEntity<UserStatsDto> getUserStats(@PathVariable Long id) {
        try {
            UserStatsDto stats = userService.getUserStats(id);
            return ResponseEntity.ok(stats);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponseDto> login(@Valid @RequestBody UserLoginDto loginDto) {
        Optional<UserResponseDto> user = userService.loginUser(loginDto);
        if (user.isPresent()) {
            return ResponseEntity.ok(user.get());
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
        Optional<UserResponseDto> user = userService.getUserById(id);
        return user.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserResponseDto>> searchUsers(@RequestParam String query) {
        List<UserResponseDto> users = userService.searchUsers(query);
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}/points")
    public ResponseEntity<UserResponseDto> updatePoints(
            @PathVariable Long id,
            @RequestParam Integer pointsChange) {
        try {
            UserResponseDto user = userService.updateUserPoints(id, pointsChange);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Leaderboard endpoints
    @GetMapping("/leaderboard/global")
    public ResponseEntity<LeaderboardResponseDto> getGlobalLeaderboard(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "allTime") String timeframe) {
        try {
            System.out.println("🌐 API: Getting global leaderboard - limit: " + limit + ", timeframe: " + timeframe);
            LeaderboardResponseDto leaderboard = userService.getGlobalLeaderboard(limit, timeframe);
            System.out.println("✅ API: Successfully retrieved leaderboard with " + leaderboard.getUsers().size() + " users");
            return ResponseEntity.ok(leaderboard);
        } catch (Exception e) {
            System.err.println("❌ API: Error getting global leaderboard: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/leaderboard/friends/{userId}")
    public ResponseEntity<List<LeaderboardUserDto>> getFriendsLeaderboard(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<LeaderboardUserDto> friendsLeaderboard = userService.getFriendsLeaderboard(userId, limit);
            return ResponseEntity.ok(friendsLeaderboard);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/leaderboard/user/{userId}/rank")
    public ResponseEntity<UserRankDto> getUserRank(@PathVariable Long userId) {
        try {
            UserRankDto userRank = userService.getUserRank(userId);
            return ResponseEntity.ok(userRank);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/leaderboard/top")
    public ResponseEntity<List<LeaderboardUserDto>> getTopPerformers(
            @RequestParam(defaultValue = "3") int limit,
            @RequestParam(defaultValue = "allTime") String timeframe) {
        try {
            System.out.println("🏆 API: Getting top performers - limit: " + limit + ", timeframe: " + timeframe);
            List<LeaderboardUserDto> topPerformers = userService.getTopPerformers(limit, timeframe);
            System.out.println("✅ API: Successfully retrieved " + topPerformers.size() + " top performers");
            return ResponseEntity.ok(topPerformers);
        } catch (Exception e) {
            System.err.println("❌ API: Error getting top performers: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
