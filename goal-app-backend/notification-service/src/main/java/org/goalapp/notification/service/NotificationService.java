package org.goalapp.notification.service;

import org.goalapp.notification.dto.NotificationDto;
import org.goalapp.notification.entities.Notification;
import org.goalapp.notification.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    public NotificationDto createNotification(Long userId, String type, String message) {
        return createNotification(userId, type, message, null);
    }

    public NotificationDto createNotification(Long userId, String type, String message, Long relatedId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setMessage(message);
        notification.setRelatedId(relatedId);
        notification.setSeen(false);
        notification.setCreatedAt(LocalDateTime.now());

        Notification saved = notificationRepository.save(notification);
        return convertToDto(saved);
    }

    public List<NotificationDto> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<NotificationDto> getUnseenNotifications(Long userId) {
        return notificationRepository.findByUserIdAndSeenOrderByCreatedAtDesc(userId, false).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public NotificationDto markAsSeen(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        notification.setSeen(true);
        Notification saved = notificationRepository.save(notification);

        return convertToDto(saved);
    }

    public void markAllAsSeen(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndSeenOrderByCreatedAtDesc(userId, false);

        for (Notification notification : notifications) {
            notification.setSeen(true);
            notificationRepository.save(notification);
        }
    }

    public Long getUnseenCount(Long userId) {
        return notificationRepository.countUnseenNotifications(userId);
    }

    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    private NotificationDto convertToDto(Notification notification) {
        NotificationDto dto = new NotificationDto();
        dto.setId(notification.getId());
        dto.setUserId(notification.getUserId());
        dto.setType(notification.getType());
        dto.setMessage(notification.getMessage());
        dto.setSeen(notification.getSeen());
        dto.setCreatedAt(notification.getCreatedAt());
        dto.setRelatedId(notification.getRelatedId());
        return dto;
    }
}
