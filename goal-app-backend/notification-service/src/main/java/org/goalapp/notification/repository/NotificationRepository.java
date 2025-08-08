package org.goalapp.notification.repository;

import org.goalapp.notification.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Notification> findByUserIdAndSeenOrderByCreatedAtDesc(Long userId, Boolean seen);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = ?1 AND n.seen = false")
    Long countUnseenNotifications(Long userId);

    List<Notification> findByType(String type);
}
