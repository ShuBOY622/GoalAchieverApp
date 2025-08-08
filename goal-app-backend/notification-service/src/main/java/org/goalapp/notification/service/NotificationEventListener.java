package org.goalapp.notification.service;

import org.goalapp.common.dto.NotificationEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    @Autowired
    private NotificationService notificationService;

    @KafkaListener(topics = "notification-topic", groupId = "notification-service-group")
    public void handleNotificationEvent(@Payload NotificationEvent event,
                                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                        @Header(value = KafkaHeaders.RECEIVED_PARTITION, required = false) Integer partition,
                                        @Header(KafkaHeaders.OFFSET) long offset,
                                        @Header(KafkaHeaders.RECEIVED_TIMESTAMP) long timestamp,
                                        Acknowledgment acknowledgment) {
        try {
            // ✅ Log detailed message receipt information
            log.info("📨 KAFKA MESSAGE RECEIVED: topic={}, partition={}, offset={}, timestamp={}",
                    topic, partition, offset, new Date(timestamp));

            log.info("🔍 NotificationEvent details: userId={}, type={}, message={}",
                    event.getUserId(), event.getType(), event.getMessage());

            // ✅ Process the notification
            log.info("💾 Creating notification in database...");
            notificationService.createNotification(
                    event.getUserId(),
                    event.getType(),
                    event.getMessage(),
                    event.getRelatedId()
            );

            // ✅ Manual acknowledgment (since you have manual_immediate mode)
            acknowledgment.acknowledge();

            log.info("✅ Notification processed and acknowledged successfully: userId={}, type={}",
                    event.getUserId(), event.getType());

        } catch (Exception e) {
            log.error("❌ Error processing notification event at offset {}: userId={}, type={}, error={}",
                    offset, event.getUserId(), event.getType(), e.getMessage(), e);

            // ✅ Don't acknowledge on error - this will cause retry
            // acknowledgment.acknowledge(); // Commented out to trigger retry

            // ✅ Rethrow to trigger error handling
            throw new RuntimeException("Failed to process notification", e);
        }
    }
}
