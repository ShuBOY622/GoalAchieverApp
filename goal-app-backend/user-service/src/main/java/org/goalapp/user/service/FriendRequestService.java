package org.goalapp.user.service;

import org.goalapp.common.dto.NotificationEvent;
import org.goalapp.user.dto.FriendRequestDto;
import org.goalapp.user.dto.UserResponseDto;
import org.goalapp.user.entities.FriendRequest;
import org.goalapp.user.entities.User;
import org.goalapp.user.repository.FriendRequestRepository;
import org.goalapp.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FriendRequestService {

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    public FriendRequestDto sendFriendRequest(Long fromUserId, Long toUserId) {
        // Check if request already exists
        if (friendRequestRepository.findExistingRequest(fromUserId, toUserId).isPresent()) {
            throw new RuntimeException("Friend request already exists");
        }

        User fromUser = userRepository.findById(fromUserId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        User toUser = userRepository.findById(toUserId)
                .orElseThrow(() -> new RuntimeException("Recipient not found"));

        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setFromUserId(fromUserId);
        friendRequest.setToUserId(toUserId);
        friendRequest.setStatus(FriendRequest.Status.PENDING);

        FriendRequest savedRequest = friendRequestRepository.save(friendRequest);

        // Send notification
        NotificationEvent notification = new NotificationEvent(
                toUserId,
                "FRIEND_REQUEST",
                fromUser.getUsername() + " sent you a friend request",
                fromUserId
        );
        kafkaTemplate.send("notification-topic", notification);

        return convertToDto(savedRequest);
    }

    public FriendRequestDto respondToFriendRequest(Long requestId, Long userId, FriendRequest.Status status) {
        FriendRequest friendRequest = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));

        if (!friendRequest.getToUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to respond to this request");
        }

        friendRequest.setStatus(status);
        friendRequest.setRespondedAt(LocalDateTime.now());

        FriendRequest savedRequest = friendRequestRepository.save(friendRequest);

        // Send notification to sender
        User toUser = userRepository.findById(userId).orElse(null);
        if (toUser != null) {
            String message = status == FriendRequest.Status.ACCEPTED
                    ? toUser.getUsername() + " accepted your friend request"
                    : toUser.getUsername() + " rejected your friend request";

            NotificationEvent notification = new NotificationEvent(
                    friendRequest.getFromUserId(),
                    "FRIEND_REQUEST_RESPONSE",
                    message,
                    userId
            );
            kafkaTemplate.send("notification-topic", notification);
        }

        return convertToDto(savedRequest);
    }

    public List<FriendRequestDto> getPendingRequests(Long userId) {
        return friendRequestRepository.findByToUserIdAndStatus(userId, FriendRequest.Status.PENDING)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<UserResponseDto> getFriends(Long userId) {
        List<Long> friendIds = friendRequestRepository.findFriendsIds(userId);
        return userRepository.findAllById(friendIds).stream()
                .map(this::convertUserToDto)
                .collect(Collectors.toList());
    }

    private FriendRequestDto convertToDto(FriendRequest friendRequest) {
        FriendRequestDto dto = new FriendRequestDto();
        dto.setId(friendRequest.getId());
        dto.setFromUserId(friendRequest.getFromUserId());
        dto.setToUserId(friendRequest.getToUserId());
        dto.setStatus(friendRequest.getStatus().toString());
        dto.setCreatedAt(friendRequest.getCreatedAt());
        dto.setRespondedAt(friendRequest.getRespondedAt());

        // Add user details
        userRepository.findById(friendRequest.getFromUserId()).ifPresent(user ->
                dto.setFromUsername(user.getUsername()));
        userRepository.findById(friendRequest.getToUserId()).ifPresent(user ->
                dto.setToUsername(user.getUsername()));

        return dto;
    }

    private UserResponseDto convertUserToDto(User user) {
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPoints(user.getPoints());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}
