package com.journeyplus.event;

import com.journeyplus.iam.entity.User;
import com.journeyplus.iam.repository.UserRepository;
import com.journeyplus.notification.entity.Notification;
import com.journeyplus.notification.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class StatusChangeEventListener {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @EventListener
    @Transactional
    public void handleStatusChangeEvent(StatusChangeEvent event) {
        try {
            Long userId = event.getUserId();
            org.slf4j.LoggerFactory.getLogger(StatusChangeEventListener.class)
                .info("Received StatusChangeEvent for userId={} title={}", userId, event.getTitle());

            User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found for notification: " + userId));

            Notification notification = new Notification(user, event.getTitle(), event.getMessage(), event.getActorId(), event.getActorName());
            notificationRepository.save(notification);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(StatusChangeEventListener.class)
                .error("Failed to handle StatusChangeEvent: {}", e.getMessage());
        }
    }
}
