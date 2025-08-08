package org.goalapp.user.controllers;

import org.goalapp.user.dto.FriendRequestDto;
import org.goalapp.user.dto.UserResponseDto;
import org.goalapp.user.entities.FriendRequest;
import org.goalapp.user.service.FriendRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friend-requests")
public class FriendRequestController {

    @Autowired
    private FriendRequestService friendRequestService;

    @PostMapping("/send")
    public ResponseEntity<FriendRequestDto> sendFriendRequest(
            @RequestParam Long fromUserId,
            @RequestParam Long toUserId) {
        try {
            FriendRequestDto friendRequest = friendRequestService.sendFriendRequest(fromUserId, toUserId);
            return ResponseEntity.ok(friendRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{requestId}/respond")
    public ResponseEntity<FriendRequestDto> respondToFriendRequest(
            @PathVariable Long requestId,
            @RequestParam Long userId,
            @RequestParam String status) {
        try {
            FriendRequest.Status requestStatus = FriendRequest.Status.valueOf(status.toUpperCase());
            FriendRequestDto friendRequest = friendRequestService.respondToFriendRequest(requestId, userId, requestStatus);
            return ResponseEntity.ok(friendRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/pending/{userId}")
    public ResponseEntity<List<FriendRequestDto>> getPendingRequests(@PathVariable Long userId) {
        List<FriendRequestDto> requests = friendRequestService.getPendingRequests(userId);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/friends/{userId}")
    public ResponseEntity<List<UserResponseDto>> getFriends(@PathVariable Long userId) {
        List<UserResponseDto> friends = friendRequestService.getFriends(userId);
        return ResponseEntity.ok(friends);
    }
}
