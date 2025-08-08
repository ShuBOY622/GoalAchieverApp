package org.goalapp.notification.controller;

import org.goalapp.notification.dto.NotificationDto;
import org.goalapp.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @PostMapping
    public ResponseEntity<NotificationDto> createNotification(
            @RequestParam Long userId,
            @RequestParam String type,
            @RequestParam String message) {
        try {
            NotificationDto notification = notificationService.createNotification(userId, type, message);
            return ResponseEntity.ok(notification);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationDto>> getUserNotifications(@PathVariable Long userId) {
        List<NotificationDto> notifications = notificationService.getUserNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    // ✅ Keep your existing unseen endpoint
    @GetMapping("/user/{userId}/unseen")
    public ResponseEntity<List<NotificationDto>> getUnseenNotifications(@PathVariable Long userId) {
        List<NotificationDto> notifications = notificationService.getUnseenNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    // ✅ Keep your existing unseen-count endpoint
    @GetMapping("/user/{userId}/unseen-count")
    public ResponseEntity<Long> getUnseenCount(@PathVariable Long userId) {
        Long count = notificationService.getUnseenCount(userId);
        return ResponseEntity.ok(count);
    }

    // ✅ ADD: Frontend-compatible unread-count endpoint
    @GetMapping("/user/{userId}/unread-count")
    public ResponseEntity<Map<String, Integer>> getUnreadCount(@PathVariable Long userId) {
        try {
            Long count = notificationService.getUnseenCount(userId); // Reuse existing service method
            return ResponseEntity.ok(Map.of("count", count.intValue()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("count", 0));
        }
    }

    // ✅ Keep your existing mark-seen endpoint
    @PutMapping("/{notificationId}/mark-seen")
    public ResponseEntity<NotificationDto> markAsSeen(@PathVariable Long notificationId) {
        try {
            NotificationDto notification = notificationService.markAsSeen(notificationId);
            return ResponseEntity.ok(notification);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ✅ ADD: Frontend-compatible read endpoint
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId) {
        try {
            notificationService.markAsSeen(notificationId); // Reuse existing service method
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ✅ Keep your existing mark-all-seen endpoint
    @PutMapping("/user/{userId}/mark-all-seen")
    public ResponseEntity<Void> markAllAsSeen(@PathVariable Long userId) {
        notificationService.markAllAsSeen(userId);
        return ResponseEntity.ok().build();
    }

    // ✅ ADD: Frontend-compatible read-all endpoint
    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<Void> markAllAsRead(@PathVariable Long userId) {
        notificationService.markAllAsSeen(userId); // Reuse existing service method
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long notificationId) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok().build();
    }
}
