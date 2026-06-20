package com.journeyplus.notification.entity;
 
import com.journeyplus.iam.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
 
@Entity
@Table(name = "notifications")
@Getter
@Setter
public class Notification {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
 
    @Column(nullable = false, length = 150)
    private String title;
 
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
 
    @Column(name = "is_read")
    private boolean read = false;
 
    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType = "IN_APP"; // IN_APP, EMAIL_SIMULATED
 
    @Column(nullable = false, length = 50)
    private String status = "SENT"; // PENDING, SENT, FAILED
 
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
 
    @Column(name = "actor_id")
    private Long actorId;
 
    @Column(name = "actor_name", length = 150)
    private String actorName;
 
    public Notification() {}
 
    public Notification(User user, String title, String message) {
        this(user, title, message, null, null);
    }
 
    public Notification(User user, String title, String message, Long actorId, String actorName) {
        this.user = user;
        this.title = title;
        this.message = message;
        this.createdAt = LocalDateTime.now();
        this.actorId = actorId;
        this.actorName = actorName;
    }
}
