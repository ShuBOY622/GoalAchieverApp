package org.goalapp.challenge.controller;

import jakarta.validation.*;
import org.goalapp.challenge.dto.ChallengeResponse;
import org.goalapp.challenge.dto.CreateChallengeRequest;
import org.goalapp.challenge.entities.Challenge;
import org.goalapp.challenge.service.ChallengeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/challenges")
public class ChallengeController {

    @Autowired
    private ChallengeService challengeService;

    @PostMapping("/send")
    public ResponseEntity<?> sendChallenge(@Valid @RequestBody CreateChallengeRequest request) {
        try {
            Challenge challenge = challengeService.sendChallenge(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(challenge);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to send challenge", "message", e.getMessage()));
        }
    }

    @PutMapping("/{challengeId}/respond")
    public ResponseEntity<?> respondToChallenge(
            @PathVariable Long challengeId,
            @RequestParam Long userId,
            @RequestParam ChallengeResponse response) {
        try {
            Challenge challenge = challengeService.respondToChallenge(challengeId, userId, response);
            return ResponseEntity.ok(challenge);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to respond to challenge", "message", e.getMessage()));
        }
    }

    @GetMapping("/pending/{userId}")
    public ResponseEntity<List<Challenge>> getPendingChallenges(@PathVariable Long userId) {
        List<Challenge> challenges = challengeService.getPendingChallenges(userId);
        return ResponseEntity.ok(challenges);
    }

    @GetMapping("/sent/{userId}")
    public ResponseEntity<List<Challenge>> getSentChallenges(@PathVariable Long userId) {
        List<Challenge> challenges = challengeService.getSentChallenges(userId);
        return ResponseEntity.ok(challenges);
    }
}
