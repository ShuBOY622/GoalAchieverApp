package org.goalapp.common.dto;

import java.time.LocalDateTime;

public class NotificationEvent {
    private Long userId;
    private String type;
    private String message;
    private Long sourceUserId;
    private Long relatedId;
    private LocalDateTime timestamp;

    // Default constructor required for deserialization
    public NotificationEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public NotificationEvent(Long userId, String type, String message, Long sourceUserId, Long relatedId) {
        this.userId = userId;
        this.type = type;
        this.message = message;
        this.sourceUserId = sourceUserId;
        this.relatedId = relatedId;
        this.timestamp = LocalDateTime.now();
    }

    public NotificationEvent(Long userId, String eventType, String goalCompleted, Long id) {
        this.userId = userId;
        this.type = eventType;
        this.message = goalCompleted;
        this.relatedId = id;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getSourceUserId() { return sourceUserId; }
    public void setSourceUserId(Long sourceUserId) { this.sourceUserId = sourceUserId; }

    public Long getRelatedId() { return relatedId; }
    public void setRelatedId(Long relatedId) { this.relatedId = relatedId; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "NotificationEvent{" +
                "userId=" + userId +
                ", type='" + type + '\'' +
                ", message='" + message + '\'' +
                ", sourceUserId=" + sourceUserId +
                ", relatedId=" + relatedId +
                ", timestamp=" + timestamp +
                '}';
    }
}
