package com.journeyplus.notification.controller;

import com.journeyplus.iam.entity.User;
import com.journeyplus.iam.repository.UserRepository;
import com.journeyplus.notification.entity.Notification;
import com.journeyplus.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getMyNotifications(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                .body(Map.of("count", 0, "notifications", List.of(), "message", "unauthorized"));
        }

        try {
            User fullUser = resolvePrincipal(user);
            Long userId = fullUser.getId();
            List<Notification> list = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
            List<Map<String, Object>> out = new java.util.ArrayList<>();
            for (Notification n : list) {
                Map<String, Object> actor = resolveActor(n.getActorId(), n.getActorName());
                String messageText = n.getMessage();
                if (messageText == null || messageText.isBlank()) {
                    if (n.getTitle() != null && !n.getTitle().isBlank()) messageText = n.getTitle();
                    else if (actor != null && actor.get("username") != null) messageText = actor.get("username") + " performed an action";
                    else messageText = "You have a new notification";
                }

                Map<String, Object> userMap = safeMapUser(n.getUser());
                Map<String, Object> userMapSafe = userMap == null ? new java.util.HashMap<>() : userMap;
                Map<String, Object> actorSafe = actor == null ? new java.util.HashMap<>() : actor;

                Map<String, Object> item = new java.util.HashMap<>();
                item.put("id", n.getId());
                item.put("user", userMapSafe);
                item.put("title", n.getTitle() == null ? "" : n.getTitle());
                item.put("message", messageText == null ? "" : messageText);
                item.put("read", n.isRead());
                item.put("notificationType", n.getNotificationType() == null ? "" : n.getNotificationType());
                item.put("status", n.getStatus() == null ? "" : n.getStatus());
                item.put("createdAt", n.getCreatedAt());
                item.put("actor", actorSafe);
                
                out.add(item);
                // out.add(item); // Removed duplicate entry
            }

            Map<String, Object> resp = Map.of(
                "count", out.size(),
                "notifications", out,
                "message", out.isEmpty() ? "No notifications" : "Notifications retrieved"
            );
            log.info("Returning {} notifications for user={}", out.size(), fullUser.getUsername());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("Failed to fetch notifications for user {}: {}", user != null ? user.getUsername() : "unknown", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("count", 0, "notifications", List.of(), "message", "Failed to fetch notifications"));
        }
    }

    @GetMapping("/unread")
    public ResponseEntity<Map<String, Object>> getMyUnreadNotifications(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                .body(Map.of("count", 0, "notifications", List.of(), "message", "unauthorized"));
        }

        try {
            User fullUser = resolvePrincipal(user);
            Long userId = fullUser.getId();
            List<Notification> list = notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
            List<Map<String, Object>> out = new java.util.ArrayList<>();
            for (Notification n : list) {
                Map<String, Object> actor = resolveActor(n.getActorId(), n.getActorName());
                String messageText = n.getMessage();
                if (messageText == null || messageText.isBlank()) {
                    if (n.getTitle() != null && !n.getTitle().isBlank()) messageText = n.getTitle();
                    else if (actor != null && actor.get("username") != null) messageText = actor.get("username") + " performed an action";
                    else messageText = "You have a new notification";
                }

                Map<String, Object> userMap = safeMapUser(n.getUser());
                Map<String, Object> userMapSafe = userMap == null ? new java.util.HashMap<>() : userMap;
                Map<String, Object> actorSafe = actor == null ? new java.util.HashMap<>() : actor;

                Map<String, Object> item = new java.util.HashMap<>();
                item.put("id", n.getId());
                item.put("user", userMapSafe);
                item.put("title", n.getTitle() == null ? "" : n.getTitle());
                item.put("message", messageText == null ? "" : messageText);
                item.put("read", n.isRead());
                item.put("notificationType", n.getNotificationType() == null ? "" : n.getNotificationType());
                item.put("status", n.getStatus() == null ? "" : n.getStatus());
                item.put("createdAt", n.getCreatedAt());
                item.put("actor", actorSafe);
                out.add(item);
            }

            Map<String, Object> resp = Map.of(
                "count", out.size(),
                "notifications", out,
                "message", out.isEmpty() ? "No unread notifications" : "Unread notifications retrieved"
            );
            log.info("Returning {} unread notifications for user={}", out.size(), fullUser.getUsername());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("Failed to fetch unread notifications for user {}: {}", user != null ? user.getUsername() : "unknown", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("count", 0, "notifications", List.of(), "message", "Failed to fetch unread notifications"));
        }
    }

    /**
     * Resolve the authentication principal into a fully-loaded User entity with id.
     * If the principal already has an id, return it. Otherwise fetch from repository by username.
     */
    private User resolvePrincipal(User principal) {
        if (principal == null) return null;
        try {
            if (principal.getId() != null) return principal;
            return userRepository.findByUsername(principal.getUsername()).orElse(principal);
        } catch (Exception e) {
            log.warn("Failed to resolve principal to full User: {}", e.getMessage());
            return principal;
        }
    }

    private Map<String, Object> safeMapUser(User u) {
        if (u == null) return null;
        try {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("email", u.getEmail());
            try { m.put("role", u.getRole()); } catch (Exception ignored) {}
            try { m.put("department", u.getDepartment()); } catch (Exception ignored) {}
            try { m.put("active", u.isActive()); } catch (Exception ignored) {}
            try { m.put("createdAt", u.getCreatedAt()); } catch (Exception ignored) {}
            try { m.put("updatedAt", u.getUpdatedAt()); } catch (Exception ignored) {}
            return m;
        } catch (Exception e) {
            // In case the User proxy can't be initialized, fall back to minimal info
            log.warn("Failed to fully map User object: {}", e.getMessage());
            try {
                Map<String, Object> m = new java.util.HashMap<>();
                m.put("id", u.getId());
                m.put("username", u.getUsername());
                return m;
            } catch (Exception ex) {
                Map<String, Object> m = new java.util.HashMap<>();
                m.put("id", null);
                m.put("username", null);
                return m;
            }
        }
    }

    // Resolve actor by id when available, otherwise fall back to actorName if provided
    private Map<String, Object> resolveActor(Long actorId, String actorName) {
        if (actorId != null) return resolveActor(actorId);
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("id", null);
        m.put("username", actorName == null ? null : actorName);
        return m;
    }

    private Map<String, Object> resolveActor(Long actorId) {
        try {
            Optional<User> u = userRepository.findById(actorId);
            if (u.isPresent()) return safeMapUser(u.get());
        } catch (Exception e) {
            log.warn("Failed to resolve actor user id {}: {}", actorId, e.getMessage());
        }
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("id", actorId);
        m.put("username", null);
        return m;
    }

    @PostMapping("/{id}/read")
    @Transactional
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long id, @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                .body(Map.of("status", "error", "message", "unauthorized"));
        }

        try {
            User fullUser = resolvePrincipal(user);
            Optional<Notification> optional = notificationRepository.findById(id);
            if (optional.isEmpty()) return ResponseEntity.status(404).body(Map.of("status", "error", "message", "Notification not found"));
            Notification notification = optional.get();
            Long ownerId = notification.getUser() != null ? notification.getUser().getId() : null;
            if (ownerId == null || !ownerId.equals(fullUser.getId())) {
                throw new org.springframework.security.access.AccessDeniedException("You are not authorized to mark this notification as read");
            }

            notification.setRead(true);
            notificationRepository.save(notification);

            Map<String, Object> body = new java.util.HashMap<>();
            body.put("status", "Notification marked as read");
            Map<String, Object> notif = new java.util.HashMap<>();
            notif.put("id", notification.getId());
            notif.put("title", notification.getTitle() == null ? "" : notification.getTitle());
            notif.put("message", notification.getMessage() == null ? "" : notification.getMessage());
            notif.put("read", notification.isRead());
            body.put("notification", notif);
            return ResponseEntity.ok(body);
        } catch (org.springframework.security.access.AccessDeniedException ade) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body(Map.of("status", "error", "message", ade.getMessage()));
        } catch (Exception e) {
            log.error("Failed to mark notification as read: {}", e.getMessage(), e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "error"));
        }
    }
}


